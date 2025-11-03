package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.jetty.ee10.servlet.SessionHandler;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.hl7.fhir.r4.model.Bundle;
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
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import de.hsheilbronn.mi.utils.test.PostgreSqlContainerLiquibaseTemplateClassRule;
import de.hsheilbronn.mi.utils.test.PostgresTemplateRule;
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
			DockerImageName.parse("postgres:18"), ROOT_USER, "fhir", "fhir_template", CHANGE_LOG_FILE,
			CHANGE_LOG_PARAMETERS, false);

	@Rule
	public final PostgresTemplateRule templateRule = new PostgresTemplateRule(liquibaseRule);

	@Rule
	public final TestNameLoggerRule testNameLoggerRule = new TestNameLoggerRule();

	private static final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);

	protected static final String CONTEXT_PATH = "/fhir";

	protected static final FhirContext fhirContext = FhirContext.forR4();
	protected static final ReadAccessHelper readAccessHelper = new ReadAccessHelperImpl();
	protected static final ProcessAuthorizationHelper processAuthorizationHelper = new ProcessAuthorizationHelperImpl();

	private static final ReferenceCleaner referenceCleaner = new ReferenceCleanerImpl(new ReferenceExtractorImpl());

	private static String baseUrl;
	private static JettyServer fhirServer;
	private static FhirWebserviceClient webserviceClient;
	private static FhirWebserviceClient externalWebserviceClient;
	private static FhirWebserviceClient practitionerWebserviceClient;
	private static FhirWebserviceClient adminWebserviceClient;
	private static FhirWebserviceClient minimalWebserviceClient;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource = createDefaultDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		defaultDataSource.unwrap(BasicDataSource.class).start();

		ServerSocketChannel statusConnectorChannel = JettyServer.serverSocketChannel("127.0.0.1");
		ServerSocketChannel apiConnectorChannel = JettyServer.serverSocketChannel("127.0.0.1");

		baseUrl = "https://localhost:" + apiConnectorChannel.socket().getLocalPort() + CONTEXT_PATH;

		logger.info("Creating webservice client ...");
		webserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getClientCertificate().trustStore(), certificates.getClientCertificate().keyStore(),
				certificates.getClientCertificate().keyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Creating external webservice client ...");
		externalWebserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getExternalClientCertificate().trustStore(),
				certificates.getExternalClientCertificate().keyStore(),
				certificates.getExternalClientCertificate().keyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Creating practitioner client ...");
		practitionerWebserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getPractitionerClientCertificate().trustStore(),
				certificates.getPractitionerClientCertificate().keyStore(),
				certificates.getPractitionerClientCertificate().keyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Creating admin client ...");
		adminWebserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getAdminClientCertificate().trustStore(),
				certificates.getAdminClientCertificate().keyStore(),
				certificates.getAdminClientCertificate().keyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Creating minimal client ...");
		minimalWebserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getMinimalClientCertificate().trustStore(),
				certificates.getMinimalClientCertificate().keyStore(),
				certificates.getMinimalClientCertificate().keyStorePassword(), fhirContext, referenceCleaner);

		logger.info("Starting FHIR Server ...");
		fhirServer = startFhirServer(statusConnectorChannel, apiConnectorChannel, baseUrl);

		logger.info("Creating template database ...");
		liquibaseRule.createTemplateDatabase();
	}

	private static FhirWebserviceClient createWebserviceClient(int apiPort, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, FhirContext fhirContext, ReferenceCleaner referenceCleaner)
	{
		return new FhirWebserviceClientJersey("https://localhost:" + apiPort + CONTEXT_PATH, trustStore, keyStore,
				keyStorePassword, null, null, null, null, Duration.ZERO, Duration.ZERO, false,
				"DSF Integration Test Client", fhirContext, referenceCleaner);
	}

	private static WebsocketClient createWebsocketClient(int apiPort, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, String subscriptionIdPart)
	{
		return new WebsocketClientTyrus(() ->
		{}, URI.create("wss://localhost:" + apiPort + CONTEXT_PATH + "/ws"), trustStore, keyStore, keyStorePassword,
				null, null, null, "Integration Test Client", subscriptionIdPart);
	}

	private static JettyServer startFhirServer(ServerSocketChannel statusConnectorChannel,
			ServerSocketChannel apiConnectorChannel, String baseUrl) throws Exception
	{
		Map<String, String> initParameters = new HashMap<>();
		initParameters.put("dev.dsf.server.status.port",
				Integer.toString(statusConnectorChannel.socket().getLocalPort()));

		initParameters.put("dev.dsf.fhir.db.url", "jdbc:postgresql://" + liquibaseRule.getHost() + ":"
				+ liquibaseRule.getMappedPort(5432) + "/" + liquibaseRule.getDatabaseName());
		initParameters.put("dev.dsf.fhir.db.user.group", DATABASE_USERS_GROUP);
		initParameters.put("dev.dsf.fhir.db.user.username", DATABASE_USER);
		initParameters.put("dev.dsf.fhir.db.user.password", DATABASE_USER_PASSWORD);
		initParameters.put("dev.dsf.fhir.db.user.permanent.delete.group", DATABASE_DELETE_USERS_GROUP);
		initParameters.put("dev.dsf.fhir.db.user.permanent.delete.username", DATABASE_DELETE_USER);
		initParameters.put("dev.dsf.fhir.db.user.permanent.delete.password", DATABASE_DELETE_USER_PASSWORD);

		initParameters.put("dev.dsf.fhir.server.base.url", baseUrl);
		initParameters.put("dev.dsf.fhir.server.organization.identifier.value", "Test_Organization");
		initParameters.put("dev.dsf.fhir.server.init.bundle", "src/test/resources/integration/test-bundle.xml");

		initParameters.put("dev.dsf.fhir.client.trust.server.certificate.cas",
				certificates.getCaCertificateFile().toString());
		initParameters.put("dev.dsf.server.auth.trust.client.certificate.cas",
				certificates.getCaCertificateFile().toString());
		initParameters.put("dev.dsf.fhir.client.certificate", certificates.getClientCertificateFile().toString());
		initParameters.put("dev.dsf.fhir.client.certificate.private.key",
				certificates.getClientCertificatePrivateKeyFile().toString());
		initParameters.put("dev.dsf.fhir.client.certificate.private.key.password",
				String.valueOf(X509Certificates.PASSWORD));

		initParameters.put("dev.dsf.fhir.server.roleConfig",
				String.format("""
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
						- admin-user:
						    thumbprint: %s
						    dsf-role: [CREATE, READ, UPDATE, DELETE, SEARCH, HISTORY]
						    practitioner-role:
						      - http://dsf.dev/fhir/CodeSystem/practitioner-role|DSF_ADMIN
						- minimal-test-user:
						    thumbprint: %s
						    dsf-role:
						      - CREATE: [Task]
						      - READ: &tqqr [Task, Questionnaire, QuestionnaireResponse]
						      - UPDATE: [QuestionnaireResponse]
						      - SEARCH: *tqqr
						      - HISTORY: *tqqr
						    practitioner-role:
						      - http://dsf.dev/fhir/CodeSystem/practitioner-role|DIC_USER
						""", certificates.getPractitionerClientCertificate().certificateSha512ThumbprintHex(),
						certificates.getAdminClientCertificate().certificateSha512ThumbprintHex(),
						certificates.getMinimalClientCertificate().certificateSha512ThumbprintHex()));
		initParameters.put("dev.dsf.fhir.debug.log.message.dbStatement", "true");

		initParameters.put("dev.dsf.fhir.server.organization.thumbprint",
				certificates.getClientCertificate().certificateSha512ThumbprintHex());
		initParameters.put("dev.dsf.fhir.server.endpoint.address",
				"https://localhost:" + apiConnectorChannel.socket().getLocalPort() + "/fhir");
		initParameters.put("dev.dsf.fhir.server.organization.thumbprint.external",
				certificates.getExternalClientCertificate().certificateSha512ThumbprintHex());
		initParameters.put("dev.dsf.fhir.server.endpoint.address.external", "https://localhost:80010/fhir");

		KeyStore clientCertificateTrustStore = KeyStoreCreator
				.jksForTrustedCertificates(certificates.getCaCertificate());
		KeyStore serverCertificateKeyStore = certificates.getServerCertificate().keyStore();

		Function<Server, ServerConnector> apiConnector = JettyServer.httpsConnector(apiConnectorChannel,
				clientCertificateTrustStore, serverCertificateKeyStore,
				certificates.getServerCertificate().keyStorePassword(), false);
		Function<Server, ServerConnector> statusConnector = JettyServer.statusConnector(statusConnectorChannel);
		List<Class<? extends ServletContainerInitializer>> servletContainerInitializers = List.of(
				JakartaWebSocketServletContainerInitializer.class, JerseyServletContainerInitializer.class,
				SpringServletContainerInitializer.class);

		BiConsumer<WebAppContext, Supplier<Integer>> securityHandlerConfigurer = (webAppContext, statusPortSupplier) ->
		{
			SessionHandler sessionHandler = webAppContext.getSessionHandler();
			DsfLoginService dsfLoginService = new DsfLoginService(webAppContext);

			StatusPortAuthenticator statusPortAuthenticator = new StatusPortAuthenticator(statusPortSupplier);
			ClientCertificateAuthenticator clientCertificateAuthenticator = new ClientCertificateAuthenticator(
					clientCertificateTrustStore);
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

	protected static IParser newXmlParser()
	{
		return newParser(fhirContext::newXmlParser);
	}

	protected static IParser newJsonParser()
	{
		return newParser(fhirContext::newJsonParser);
	}

	private static IParser newParser(Supplier<IParser> supplier)
	{
		IParser p = supplier.get();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);
		p.setPrettyPrint(true);
		return p;
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		try
		{
			if (fhirServer != null)
			{
				logger.info("Stopping FHIR Server ...");
				fhirServer.stop();
			}
		}
		catch (Exception e)
		{
			logger.error("Error while stopping FHIR Server", e);
		}

		defaultDataSource.unwrap(BasicDataSource.class).close();
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

	protected static FhirWebserviceClient getAdminWebserviceClient()
	{
		return adminWebserviceClient;
	}

	protected static FhirWebserviceClient getMinimalWebserviceClient()
	{
		return minimalWebserviceClient;
	}

	protected static WebsocketClient getWebsocketClient()
	{
		Bundle bundle = getWebserviceClient().searchWithStrictHandling(Subscription.class,
				Map.of("criteria", List.of("Task?status=requested"), "status", List.of("active"), "type",
						List.of("websocket"), "payload", List.of("application/fhir+json")));

		assertNotNull(bundle);
		assertEquals(1, bundle.getTotal());
		assertNotNull(bundle.getEntryFirstRep());
		assertTrue(bundle.getEntryFirstRep().getResource() instanceof Subscription);

		Subscription subscription = (Subscription) bundle.getEntryFirstRep().getResource();
		assertNotNull(subscription.getIdElement());
		assertNotNull(subscription.getIdElement().getIdPart());

		return createWebsocketClient(fhirServer.getApiPort(), certificates.getClientCertificate().trustStore(),
				certificates.getClientCertificate().keyStore(), certificates.getClientCertificate().keyStorePassword(),
				subscription.getIdElement().getIdPart());
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
