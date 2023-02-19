package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.sql.Connection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.jetty.websocket.jakarta.client.JakartaWebSocketShutdownContainer;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Subscription;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.rwh.utils.test.LiquibaseTemplateTestClassRule;
import de.rwh.utils.test.LiquibaseTemplateTestRule;
import dev.dsf.common.jetty.JettyConfig;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.FhirWebserviceClientJersey;
import dev.dsf.fhir.client.WebsocketClient;
import dev.dsf.fhir.client.WebsocketClientTyrus;
import dev.dsf.fhir.dao.AbstractDbTest;
import dev.dsf.fhir.integration.X509Certificates.ClientCertificate;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractorImpl;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

public abstract class AbstractIntegrationTest extends AbstractDbTest
{
	@ClassRule
	public static final X509Certificates certificates = new X509Certificates();

	public static final String INTEGRATION_TEST_DB_TEMPLATE_NAME = "integration_test_template";

	protected static final BasicDataSource adminDataSource = createAdminBasicDataSource();
	protected static final BasicDataSource liquibaseDataSource = createLiquibaseDataSource();
	protected static final BasicDataSource defaultDataSource = createDefaultDataSource();

	@ClassRule
	public static final LiquibaseTemplateTestClassRule liquibaseRule = new LiquibaseTemplateTestClassRule(
			adminDataSource, LiquibaseTemplateTestClassRule.DEFAULT_TEST_DB_NAME, INTEGRATION_TEST_DB_TEMPLATE_NAME,
			liquibaseDataSource, CHANGE_LOG_FILE, CHANGE_LOG_PARAMETERS, false);

	@Rule
	public final LiquibaseTemplateTestRule templateRule = new LiquibaseTemplateTestRule(adminDataSource,
			LiquibaseTemplateTestClassRule.DEFAULT_TEST_DB_NAME, INTEGRATION_TEST_DB_TEMPLATE_NAME);

	private static final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);

	protected static final String CONTEXT_PATH = "/fhir";
	protected static final String BASE_URL = "https://localhost:8001" + CONTEXT_PATH;
	protected static final String WEBSOCKET_URL = "wss://localhost:8001" + CONTEXT_PATH + "/ws";

	private static final Path FHIR_BUNDLE_FILE = Paths.get("target", UUID.randomUUID().toString() + ".xml");
	private static final List<Path> FILES_TO_DELETE = Arrays.asList(FHIR_BUNDLE_FILE);

	protected static final FhirContext fhirContext = FhirContext.forR4();
	protected static final ReadAccessHelperImpl readAccessHelper = new ReadAccessHelperImpl();

	private static final ReferenceCleaner referenceCleaner = new ReferenceCleanerImpl(new ReferenceExtractorImpl());

	private static JettyServer fhirServer;
	private static FhirWebserviceClient webserviceClient;
	private static FhirWebserviceClient externalWebserviceClient;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource.start();
		liquibaseDataSource.start();
		adminDataSource.start();

		logger.info("Creating Bundle ...");
		createTestBundle(certificates.getClientCertificate(), certificates.getExternalClientCertificate());

		logger.info("Creating webservice client ...");
		webserviceClient = createWebserviceClient(certificates.getClientCertificate().getTrustStore(),
				certificates.getClientCertificate().getKeyStore(),
				certificates.getClientCertificate().getKeyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Creating external webservice client ...");
		externalWebserviceClient = createWebserviceClient(certificates.getExternalClientCertificate().getTrustStore(),
				certificates.getExternalClientCertificate().getKeyStore(),
				certificates.getExternalClientCertificate().getKeyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Starting FHIR Server ...");
		fhirServer = startFhirServer();

		logger.info("Creating template database ...");
		try (Connection connection = adminDataSource.getConnection())
		{
			liquibaseRule.createTemplateDatabase(connection);
		}
	}

	private static FhirWebserviceClient createWebserviceClient(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, FhirContext fhirContext, ReferenceCleaner referenceCleaner)
	{
		return new FhirWebserviceClientJersey(BASE_URL, trustStore, keyStore, keyStorePassword, null, null, null, 0, 0,
				false, null, fhirContext, referenceCleaner);
	}

	private static WebsocketClient createWebsocketClient(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, String subscriptionIdPart)
	{
		return new WebsocketClientTyrus(() ->
		{}, URI.create(WEBSOCKET_URL), trustStore, keyStore, keyStorePassword, null, null, null, subscriptionIdPart);
	}

	private static JettyServer startFhirServer() throws Exception
	{
		JettyConfig jettyConfig = new TestJettyConfig(10001, 8001, CONTEXT_PATH, certificates.getCaCertificateFile(),
				certificates.getCaCertificateFile(), certificates.getServerCertificateFile(), X509Certificates.PASSWORD,
				Paths.get("log4j2.xml"), DATABASE_URL, DATABASE_USER, DATABASE_USER_PASSWORD, DATABASE_DELETE_USER,
				DATABASE_DELETE_USER_PASSWORD, BASE_URL, certificates.getClientCertificate(), "Test_Organization",
				FHIR_BUNDLE_FILE, certificates.getCaCertificateFile(), certificates.getClientCertificateFile(),
				certificates.getClientCertificatePrivateKeyFile(), X509Certificates.PASSWORD);

		JettyServer server = new JettyServer("fhir-server", jettyConfig,
				Stream.of(JakartaWebSocketShutdownContainer.class, JakartaWebSocketServletContainerInitializer.class,
						JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class));

		server.start();

		return server;
	}

	protected static Bundle readBundle(Path bundleTemplateFile, IParser parser)
	{
		try (InputStream in = Files.newInputStream(bundleTemplateFile))
		{
			Bundle bundle = parser.parseResource(Bundle.class, in);
			return referenceCleaner.cleanReferenceResourcesIfBundle(bundle);
		}
		catch (IOException e)
		{
			logger.error("Error while reading bundle from " + bundleTemplateFile.toString(), e);
			throw new RuntimeException(e);
		}
	}

	protected static void writeBundle(Path bundleFile, Bundle bundle)
	{
		try (OutputStream out = Files.newOutputStream(bundleFile);
				OutputStreamWriter writer = new OutputStreamWriter(out))
		{
			newXmlParser().encodeResourceToWriter(bundle, writer);
		}
		catch (IOException e)
		{
			logger.error("Error while writing bundle to " + bundleFile.toString(), e);
			throw new RuntimeException(e);
		}
	}

	protected static IParser newXmlParser()
	{
		IParser parser = fhirContext.newXmlParser();
		parser.setStripVersionsFromReferences(false);
		parser.setOverrideResourceIdWithBundleEntryFullUrl(false);
		parser.setPrettyPrint(true);
		return parser;
	}

	protected static IParser newJsonParser()
	{
		IParser parser = fhirContext.newJsonParser();
		parser.setStripVersionsFromReferences(false);
		parser.setOverrideResourceIdWithBundleEntryFullUrl(false);
		parser.setPrettyPrint(true);
		return parser;
	}

	private static void createTestBundle(ClientCertificate clientCertificate,
			ClientCertificate externalClientCertificate)
	{
		Path testBundleTemplateFile = Paths.get("src/test/resources/integration/test-bundle.xml");

		Bundle testBundle = readBundle(testBundleTemplateFile, newXmlParser());

		Organization organization = (Organization) testBundle.getEntry().get(0).getResource();
		Extension thumbprintExtension = organization
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint");

		thumbprintExtension.setValue(new StringType(clientCertificate.getCertificateSha512ThumbprintHex()));

		Organization externalOrganization = (Organization) testBundle.getEntry().get(2).getResource();
		Extension externalThumbprintExtension = externalOrganization
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint");

		externalThumbprintExtension
				.setValue(new StringType(externalClientCertificate.getCertificateSha512ThumbprintHex()));

		// FIXME hapi parser can't handle embedded resources and creates them while parsing bundles
		new ReferenceCleanerImpl(new ReferenceExtractorImpl()).cleanReferenceResourcesIfBundle(testBundle);

		writeBundle(FHIR_BUNDLE_FILE, testBundle);
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		defaultDataSource.close();
		liquibaseDataSource.close();
		adminDataSource.close();

		try
		{
			if (fhirServer != null)
			{
				logger.info("Stoping FHIR Server ...");
				fhirServer.stop();
			}
		}
		catch (Exception e)
		{
			logger.error("Error while stopping FHIR Server", e);
		}

		logger.info("Deleting files {} ...", FILES_TO_DELETE);
		FILES_TO_DELETE.forEach(AbstractIntegrationTest::deleteFile);
	}

	private static void deleteFile(Path file)
	{
		try
		{
			Files.delete(file);
		}
		catch (IOException e)
		{
			logger.error("Error while deleting test file {}, error: {}", file.toString(), e.toString());
		}
	}

	protected AnnotationConfigWebApplicationContext getSpringWebApplicationContext()
	{
		return (AnnotationConfigWebApplicationContext) WebApplicationContextUtils
				.getWebApplicationContext(fhirServer.getServletContext());
	}

	protected static FhirWebserviceClient getWebserviceClient()
	{
		return webserviceClient;
	}

	protected static FhirWebserviceClient getExternalWebserviceClient()
	{
		return externalWebserviceClient;
	}

	protected static WebsocketClient getWebsocketClient()
	{
		Bundle bundle = getWebserviceClient().searchWithStrictHandling(Subscription.class,
				Map.of("criteria", Collections.singletonList("Task?status=requested"), "status",
						Collections.singletonList("active"), "type", Collections.singletonList("websocket"), "payload",
						Collections.singletonList("application/fhir+json")));

		assertNotNull(bundle);
		assertEquals(1, bundle.getTotal());
		assertNotNull(bundle.getEntryFirstRep());
		assertTrue(bundle.getEntryFirstRep().getResource() instanceof Subscription);

		Subscription subscription = (Subscription) bundle.getEntryFirstRep().getResource();
		assertNotNull(subscription.getIdElement());
		assertNotNull(subscription.getIdElement().getIdPart());

		return createWebsocketClient(certificates.getClientCertificate().getTrustStore(),
				certificates.getClientCertificate().getKeyStore(),
				certificates.getClientCertificate().getKeyStorePassword(), subscription.getIdElement().getIdPart());
	}

	protected static final ReadAccessHelper getReadAccessHelper()
	{
		return readAccessHelper;
	}

	protected static void expectBadRequest(Runnable operation) throws Exception
	{
		expectWebApplicationException(operation, Status.BAD_REQUEST);
	}

	protected static void expectForbidden(Runnable operation) throws Exception
	{
		expectWebApplicationException(operation, Status.FORBIDDEN);
	}

	protected static void expectNotFound(Runnable operation) throws Exception
	{
		expectWebApplicationException(operation, Status.NOT_FOUND);
	}

	protected static void expectNotAcceptable(Runnable operation) throws Exception
	{
		expectWebApplicationException(operation, Status.NOT_ACCEPTABLE);
	}

	protected static void expectWebApplicationException(Runnable operation, Status status) throws Exception
	{
		try
		{
			operation.run();
			fail("WebApplicationException expected");
		}
		catch (WebApplicationException e)
		{
			assertEquals(status.getStatusCode(), e.getResponse().getStatus());
		}
	}
}
