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
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.webapp.WebAppContext;
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
import org.testcontainers.utility.DockerImageName;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import de.hsheilbronn.mi.utils.test.PostgreSqlContainerLiquibaseTemplateClassRule;
import de.hsheilbronn.mi.utils.test.PostgresTemplateRule;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.common.auth.ClientCertificateAuthenticator;
import dev.dsf.common.auth.DelegatingAuthenticator;
import dev.dsf.common.auth.DsfLoginService;
import dev.dsf.common.auth.DsfSecurityHandler;
import dev.dsf.common.auth.StatusPortAuthenticator;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelperImpl;
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
import jakarta.servlet.ServletContainerInitializer;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

public abstract class AbstractIntegrationTest extends AbstractDbTest
{
	@ClassRule
	public static final X509Certificates certificates = new X509Certificates();

	protected static DataSource defaultDataSource;

	@ClassRule
	public static final PostgreSqlContainerLiquibaseTemplateClassRule liquibaseRule = new PostgreSqlContainerLiquibaseTemplateClassRule(
			DockerImageName.parse("postgres:15"), ROOT_USER, "fhir", "fhir_template", CHANGE_LOG_FILE,
			CHANGE_LOG_PARAMETERS, false);

	@Rule
	public final PostgresTemplateRule templateRule = new PostgresTemplateRule(liquibaseRule);

	@Rule
	public final TestNameLoggerRule testNameLoggerRule = new TestNameLoggerRule();

	private static final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);

	protected static final String CONTEXT_PATH = "/fhir";
	protected static final String WEBSOCKET_URL = "wss://localhost:8001" + CONTEXT_PATH + "/ws";

	private static final Path FHIR_BUNDLE_FILE = Paths.get("target", UUID.randomUUID().toString() + ".xml");
	private static final List<Path> FILES_TO_DELETE = Arrays.asList(FHIR_BUNDLE_FILE);

	protected static final FhirContext fhirContext = FhirContext.forR4();
	protected static final ReadAccessHelper readAccessHelper = new ReadAccessHelperImpl();
	protected static final ProcessAuthorizationHelper processAuthorizationHelper = new ProcessAuthorizationHelperImpl();

	private static final ReferenceCleaner referenceCleaner = new ReferenceCleanerImpl(new ReferenceExtractorImpl());

	private static String baseUrl;
	private static JettyServer fhirServer;
	private static FhirWebserviceClient webserviceClient;
	private static FhirWebserviceClient externalWebserviceClient;
	private static FhirWebserviceClient practitionerWebserviceClient;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource = createDefaultDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		defaultDataSource.unwrap(BasicDataSource.class).start();

		logger.info("Creating Bundle ...");
		createTestBundle(certificates.getClientCertificate(), certificates.getExternalClientCertificate());

		ServerSocketChannel statusConnectorChannel = JettyServer.serverSocketChannel();
		ServerSocketChannel apiConnectorChannel = JettyServer.serverSocketChannel();

		baseUrl = "https://localhost:" + apiConnectorChannel.socket().getLocalPort() + CONTEXT_PATH;

		logger.info("Creating webservice client ...");
		webserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getClientCertificate().getTrustStore(), certificates.getClientCertificate().getKeyStore(),
				certificates.getClientCertificate().getKeyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Creating external webservice client ...");
		externalWebserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getExternalClientCertificate().getTrustStore(),
				certificates.getExternalClientCertificate().getKeyStore(),
				certificates.getExternalClientCertificate().getKeyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Creating practitioner client ...");
		practitionerWebserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getPractitionerClientCertificate().getTrustStore(),
				certificates.getPractitionerClientCertificate().getKeyStore(),
				certificates.getPractitionerClientCertificate().getKeyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Starting FHIR Server ...");
		fhirServer = startFhirServer(statusConnectorChannel, apiConnectorChannel, baseUrl);

		logger.info("Creating template database ...");
		liquibaseRule.createTemplateDatabase();
	}

	private static FhirWebserviceClient createWebserviceClient(int apiPort, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, FhirContext fhirContext, ReferenceCleaner referenceCleaner)
	{
		return new FhirWebserviceClientJersey("https://localhost:" + apiPort + CONTEXT_PATH, trustStore, keyStore,
				keyStorePassword, null, null, null, null, 0, 0, false, "DSF Integration Test Client", fhirContext,
				referenceCleaner);
	}

	private static WebsocketClient createWebsocketClient(KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, String subscriptionIdPart)
	{
		return new WebsocketClientTyrus(() ->
		{}, URI.create(WEBSOCKET_URL), trustStore, keyStore, keyStorePassword, null, null, null,
				"Integration Test Client", subscriptionIdPart);
	}

	private static JettyServer startFhirServer(ServerSocketChannel statusConnectorChannel,
			ServerSocketChannel apiConnectorChannel, String baseUrl) throws Exception
	{
		Map<String, String> initParameters = new HashMap<>();
		initParameters.put("dev.dsf.server.status.port",
				Integer.toString(statusConnectorChannel.socket().getLocalPort()));

		initParameters.put("dev.dsf.fhir.db.url", "jdbc:postgresql://" + liquibaseRule.getHost() + ":"
				+ liquibaseRule.getMappedPort(5432) + "/" + liquibaseRule.getDatabaseName());
		initParameters.put("dev.dsf.fhir.db.user.username", DATABASE_USER);
		initParameters.put("dev.dsf.fhir.db.user.password", DATABASE_USER_PASSWORD);
		initParameters.put("dev.dsf.fhir.db.user.permanent.delete.username", DATABASE_DELETE_USER);
		initParameters.put("dev.dsf.fhir.db.user.permanent.delete.password", DATABASE_DELETE_USER_PASSWORD);

		initParameters.put("dev.dsf.fhir.server.base.url", baseUrl);
		initParameters.put("dev.dsf.fhir.server.organization.identifier.value", "Test_Organization");
		initParameters.put("dev.dsf.fhir.server.init.bundle", FHIR_BUNDLE_FILE.toString());

		initParameters.put("dev.dsf.fhir.client.trust.server.certificate.cas",
				certificates.getCaCertificateFile().toString());
		initParameters.put("dev.dsf.fhir.client.certificate", certificates.getClientCertificateFile().toString());
		initParameters.put("dev.dsf.fhir.client.certificate.private.key",
				certificates.getClientCertificatePrivateKeyFile().toString());
		initParameters.put("dev.dsf.fhir.client.certificate.private.key.password",
				String.valueOf(X509Certificates.PASSWORD));

		initParameters.put("dev.dsf.fhir.server.roleConfig", String.format("""
				- practitioner-test-user:
				    thumbprint: %s
				    dsf-role:
				      - CREATE
				      - READ
				      - UPDATE
				      - DELETE
				      - SEARCH
				      - HISTORY
				    practitioner-role:
				      - http://dsf.dev/fhir/CodeSystem/practitioner-role|DIC_USER
				""", certificates.getPractitionerClientCertificate().getCertificateSha512ThumbprintHex()));

		KeyStore caCertificate = CertificateReader.allFromCer(certificates.getCaCertificateFile());
		PrivateKey privateKey = PemIo.readPrivateKeyFromPem(certificates.getServerCertificatePrivateKeyFile(),
				X509Certificates.PASSWORD);
		X509Certificate certificate = PemIo.readX509CertificateFromPem(certificates.getServerCertificateFile());
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore serverCertificateKeyStore = CertificateHelper.toJksKeyStore(privateKey,
				new Certificate[] { certificate }, UUID.randomUUID().toString(), keyStorePassword);

		Function<Server, ServerConnector> apiConnector = JettyServer.httpsConnector(apiConnectorChannel, caCertificate,
				serverCertificateKeyStore, keyStorePassword, false);
		Function<Server, ServerConnector> statusConnector = JettyServer.statusConnector(statusConnectorChannel);
		List<Class<? extends ServletContainerInitializer>> servletContainerInitializers = Arrays.asList(
				JakartaWebSocketShutdownContainer.class, JakartaWebSocketServletContainerInitializer.class,
				JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class);

		BiConsumer<WebAppContext, Supplier<Integer>> securityHandlerConfigurer = (webAppContext, statusPortSupplier) ->
		{
			SessionHandler sessionHandler = webAppContext.getSessionHandler();
			DsfLoginService dsfLoginService = new DsfLoginService(webAppContext);

			StatusPortAuthenticator statusPortAuthenticator = new StatusPortAuthenticator(statusPortSupplier);
			ClientCertificateAuthenticator clientCertificateAuthenticator = new ClientCertificateAuthenticator(
					caCertificate);
			DelegatingAuthenticator delegatingAuthenticator = new DelegatingAuthenticator(sessionHandler,
					statusPortAuthenticator, clientCertificateAuthenticator, null, null, null, null);

			SecurityHandler securityHandler = new DsfSecurityHandler(dsfLoginService, delegatingAuthenticator, null);
			securityHandler.setSessionRenewedOnAuthentication(true);

			webAppContext.setSecurityHandler(securityHandler);
		};

		JettyServer server = new JettyServer(apiConnector, statusConnector, "dsf-fhir-server", CONTEXT_PATH,
				servletContainerInitializers, initParameters, securityHandlerConfigurer);

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
			logger.error("Error while reading bundle from {}", bundleTemplateFile.toString(), e);
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
			logger.error("Error while writing bundle to {}", bundleFile.toString(), e);
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
		defaultDataSource.unwrap(BasicDataSource.class).close();

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

	protected static String getBaseUrl()
	{
		return baseUrl;
	}

	protected static FhirWebserviceClient getWebserviceClient()
	{
		return webserviceClient;
	}

	protected static FhirWebserviceClient getExternalWebserviceClient()
	{
		return externalWebserviceClient;
	}

	protected static FhirWebserviceClient getPractitionerWebserviceClient()
	{
		return practitionerWebserviceClient;
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

	protected static final ProcessAuthorizationHelper getProcessAuthorizationHelper()
	{
		return processAuthorizationHelper;
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

	protected static void expectGone(Runnable operation) throws Exception
	{
		expectWebApplicationException(operation, Status.GONE);
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
