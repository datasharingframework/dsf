/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.ServerSocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.eclipse.jetty.ee11.servlet.SessionHandler;
import org.eclipse.jetty.ee11.webapp.WebAppContext;
import org.eclipse.jetty.ee11.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
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
import dev.dsf.bpe.dao.AbstractDbTest;
import dev.dsf.common.auth.ClientCertificateAuthenticator;
import dev.dsf.common.auth.DelegatingAuthenticator;
import dev.dsf.common.auth.DsfLoginService;
import dev.dsf.common.auth.DsfSecurityHandler;
import dev.dsf.common.auth.StatusPortAuthenticator;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.FhirWebserviceClientJersey;
import dev.dsf.fhir.client.WebsocketClient;
import dev.dsf.fhir.client.WebsocketClientTyrus;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractorImpl;
import jakarta.servlet.ServletContainerInitializer;

public abstract class AbstractIntegrationTest extends AbstractDbTest
{
	@ClassRule
	public static final X509Certificates certificates = new X509Certificates();

	protected static DataSource fhirDefaultDataSource;
	protected static DataSource bpeDefaultDataSource;

	@ClassRule
	public static final PostgreSqlContainerLiquibaseTemplateClassRule bpeLiquibaseRule = new PostgreSqlContainerLiquibaseTemplateClassRule(
			DockerImageName.parse("postgres:18"), ROOT_USER, "bpe", "bpe_template", BPE_CHANGE_LOG_FILE,
			BPE_CHANGE_LOG_PARAMETERS, false);

	@Rule
	public final PostgresTemplateRule bpeTemplateRule = new PostgresTemplateRule(bpeLiquibaseRule);

	@ClassRule
	public static final PostgreSqlContainerLiquibaseTemplateClassRule fhirLiquibaseRule = new PostgreSqlContainerLiquibaseTemplateClassRule(
			DockerImageName.parse("postgres:18"), ROOT_USER, "fhir", "fhir_template", FHIR_CHANGE_LOG_FILE,
			FHIR_CHANGE_LOG_PARAMETERS, false);

	@Rule
	public final PostgresTemplateRule fhirTemplateRule = new PostgresTemplateRule(fhirLiquibaseRule);

	@Rule
	public final TestNameLoggerRule testNameLoggerRule = new TestNameLoggerRule();

	private static final Logger logger = LoggerFactory.getLogger(AbstractIntegrationTest.class);

	protected static final String FHIR_CONTEXT_PATH = "/fhir";
	protected static final String BPE_CONTEXT_PATH = "/bpe";

	private static final Path EMPTY_PROCESS_DIRECTORY = Paths.get("target", UUID.randomUUID().toString());
	private static final List<Path> DIRECTORIES_TO_DELETE = List.of(EMPTY_PROCESS_DIRECTORY);

	private static final Path ALLOWED_BPE_CLASSES_LIST_FILE_V1 = Paths.get("target",
			UUID.randomUUID().toString() + ".list");
	private static final Path ALLOWED_BPE_CLASSES_LIST_FILE_V2 = Paths.get("target",
			UUID.randomUUID().toString() + ".list");
	private static final List<Path> FILES_TO_DELETE = List.of(ALLOWED_BPE_CLASSES_LIST_FILE_V1,
			ALLOWED_BPE_CLASSES_LIST_FILE_V2);

	protected static final FhirContext fhirContext = FhirContext.forR4();

	private static final ReferenceCleaner referenceCleaner = new ReferenceCleanerImpl(new ReferenceExtractorImpl());

	protected static TestFhirDataServer testFhirDataServer;

	private static JettyServer fhirServer;
	private static FhirWebserviceClient webserviceClient;
	private static JettyServer bpeServer;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		fhirDefaultDataSource = createFhirDefaultDataSource(fhirLiquibaseRule.getHost(),
				fhirLiquibaseRule.getMappedPort(5432), fhirLiquibaseRule.getDatabaseName());
		fhirDefaultDataSource.unwrap(BasicDataSource.class).start();

		ServerSocketChannel fhirStatusConnectorChannel = JettyServer.serverSocketChannel("127.0.0.1");
		ServerSocketChannel fhirApiConnectorChannel = JettyServer.serverSocketChannel("127.0.0.1");

		String fhirBaseUrl = "https://localhost:" + fhirApiConnectorChannel.socket().getLocalPort() + FHIR_CONTEXT_PATH;

		logger.info("Creating webservice client ...");
		webserviceClient = createWebserviceClient(fhirBaseUrl, certificates.getClientCertificate().trustStore(),
				certificates.getClientCertificate().keyStore(), certificates.getClientCertificate().keyStorePassword(),
				fhirContext, referenceCleaner);

		logger.info("Starting FHIR Server ...");
		fhirServer = startFhirServer(fhirStatusConnectorChannel, fhirApiConnectorChannel, fhirBaseUrl);

		// allowed bpe classes override to enable access to classes from dsf-bpe-test-plugin module for v1 test plugins
		List<String> allowedBpeClassesV1 = readListFile(
				Paths.get("src/main/resources/bpe/api/v1/allowed-bpe-classes.list"));
		allowedBpeClassesV1.add("dev.dsf.bpe.test.PluginTest");
		allowedBpeClassesV1.add("dev.dsf.bpe.test.PluginTestExecutor");
		allowedBpeClassesV1.add("dev.dsf.bpe.test.PluginTestExecutor$RunnableWithException");
		writeListFile(ALLOWED_BPE_CLASSES_LIST_FILE_V1, allowedBpeClassesV1);

		// allowed bpe classes override to enable access to classes from dsf-bpe-test-plugin module for v2 test plugins
		List<String> allowedBpeClassesV2 = readListFile(
				Paths.get("src/main/resources/bpe/api/v2/allowed-bpe-classes.list"));
		allowedBpeClassesV2.add("dev.dsf.bpe.test.PluginTest");
		allowedBpeClassesV2.add("dev.dsf.bpe.test.PluginTestExecutor");
		allowedBpeClassesV2.add("dev.dsf.bpe.test.PluginTestExecutor$RunnableWithException");
		writeListFile(ALLOWED_BPE_CLASSES_LIST_FILE_V2, allowedBpeClassesV2);

		bpeDefaultDataSource = createBpeDefaultDataSource(bpeLiquibaseRule.getHost(),
				bpeLiquibaseRule.getMappedPort(5432), bpeLiquibaseRule.getDatabaseName());
		bpeDefaultDataSource.unwrap(BasicDataSource.class).start();

		ServerSocketChannel bpeStatusConnectorChannel = JettyServer.serverSocketChannel("127.0.0.1");
		ServerSocketChannel bpeApiConnectorChannel = JettyServer.serverSocketChannel("127.0.0.1");

		String bpeBaseUrl = "https://localhost:" + bpeApiConnectorChannel.socket().getLocalPort() + BPE_CONTEXT_PATH;

		Files.createDirectories(EMPTY_PROCESS_DIRECTORY);

		testFhirDataServer = new TestFhirDataServer(certificates.getFhirServerCertificate());

		logger.info("Starting BPE Server ...");
		bpeServer = startBpeServer(bpeStatusConnectorChannel, bpeApiConnectorChannel, bpeBaseUrl, fhirBaseUrl);

		logger.info("Creating FHIR template database ...");
		fhirLiquibaseRule.createTemplateDatabase();

		logger.info("Creating BPE template database ...");
		bpeLiquibaseRule.createTemplateDatabase();

		// wait for bpe to fhir websocket connections
		Thread.sleep(Duration.ofSeconds(1));
	}

	private static FhirWebserviceClient createWebserviceClient(String fhirBaseUrl, KeyStore trustStore,
			KeyStore keyStore, char[] keyStorePassword, FhirContext fhirContext, ReferenceCleaner referenceCleaner)
	{
		return new FhirWebserviceClientJersey(fhirBaseUrl, trustStore, keyStore, keyStorePassword, null, null, null,
				null, Duration.ZERO, Duration.ZERO, false, "DSF Integration Test Client", fhirContext,
				referenceCleaner);
	}

	protected static FhirWebserviceClient getWebserviceClient()
	{
		return webserviceClient;
	}

	protected static WebsocketClient getWebsocketClient()
	{
		Bundle bundle = getWebserviceClient().searchWithStrictHandling(Subscription.class,
				Map.of("criteria:exact",
						List.of("Task?_profile:below=http://dsf.dev/fhir/StructureDefinition/task-test"), "status",
						List.of("active"), "type", List.of("websocket"), "payload", List.of("application/fhir+json")));

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

	private static WebsocketClient createWebsocketClient(int fhirApiPort, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword, String subscriptionIdPart)
	{
		return new WebsocketClientTyrus(() ->
		{}, URI.create("wss://localhost:" + fhirApiPort + FHIR_CONTEXT_PATH + "/ws"), trustStore, keyStore,
				keyStorePassword, null, null, null, "Integration Test Client", subscriptionIdPart);
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

	private static JettyServer startFhirServer(ServerSocketChannel statusConnectorChannel,
			ServerSocketChannel apiConnectorChannel, String baseUrl) throws Exception
	{
		Map<String, String> initParameters = new HashMap<>();
		initParameters.put("dev.dsf.server.status.port",
				Integer.toString(statusConnectorChannel.socket().getLocalPort()));

		initParameters.put("dev.dsf.fhir.db.url", "jdbc:postgresql://" + fhirLiquibaseRule.getHost() + ":"
				+ fhirLiquibaseRule.getMappedPort(5432) + "/" + fhirLiquibaseRule.getDatabaseName());
		initParameters.put("dev.dsf.fhir.db.user.group", FHIR_DATABASE_USERS_GROUP);
		initParameters.put("dev.dsf.fhir.db.user.username", FHIR_DATABASE_USER);
		initParameters.put("dev.dsf.fhir.db.user.password", FHIR_DATABASE_USER_PASSWORD);
		initParameters.put("dev.dsf.fhir.db.user.permanent.delete.group", FHIR_DATABASE_DELETE_USERS_GROUP);
		initParameters.put("dev.dsf.fhir.db.user.permanent.delete.username", FHIR_DATABASE_DELETE_USER);
		initParameters.put("dev.dsf.fhir.db.user.permanent.delete.password", FHIR_DATABASE_DELETE_USER_PASSWORD);

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

		initParameters.put("dev.dsf.fhir.server.roleConfig", String.format("""
				- dic-user:
				    thumbprint: %s
				    dsf-role:
				      - CREATE: [Task]
				      - READ: &tqqr [Task, Questionnaire, QuestionnaireResponse]
				      - UPDATE: [QuestionnaireResponse]
				      - SEARCH: *tqqr
				      - HISTORY: *tqqr
				    practitioner-role:
				      - http://dsf.dev/fhir/CodeSystem/practitioner-role|DIC_USER
				- uac-user:
				    thumbprint: %s
				    dsf-role:
				      - CREATE: [Task]
				      - READ: &tqqr [Task, Questionnaire, QuestionnaireResponse]
				      - UPDATE: [QuestionnaireResponse]
				      - SEARCH: *tqqr
				      - HISTORY: *tqqr
				    practitioner-role:
				      - http://dsf.dev/fhir/CodeSystem/practitioner-role|UAC_USER
				""", certificates.getDicUserClientCertificate().certificateSha512ThumbprintHex(),
				certificates.getUacUserClientCertificate().certificateSha512ThumbprintHex()));

		initParameters.put("dev.dsf.fhir.server.organization.thumbprint",
				certificates.getClientCertificate().certificateSha512ThumbprintHex());
		initParameters.put("dev.dsf.fhir.server.endpoint.address",
				"https://localhost:" + apiConnectorChannel.socket().getLocalPort() + "/fhir");
		initParameters.put("dev.dsf.fhir.server.organization.thumbprint.external",
				certificates.getExternalClientCertificate().certificateSha512ThumbprintHex());
		initParameters.put("dev.dsf.fhir.server.endpoint.address.external", "https://localhost:80010/fhir");

		KeyStore clientCertificateTrustStore = KeyStoreCreator
				.jksForTrustedCertificates(certificates.getCaCertificate());
		KeyStore fhirServerCertificateKeyStore = certificates.getFhirServerCertificate().keyStore();

		Function<Server, ServerConnector> apiConnector = JettyServer.httpsConnector(apiConnectorChannel,
				clientCertificateTrustStore, fhirServerCertificateKeyStore,
				certificates.getFhirServerCertificate().keyStorePassword(), false);
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

		JettyServer server = new JettyServer(apiConnector, statusConnector, "dsf-fhir-server", FHIR_CONTEXT_PATH,
				servletContainerInitializers, initParameters, securityHandlerConfigurer);

		server.start();

		return server;
	}

	private static JettyServer startBpeServer(ServerSocketChannel statusConnectorChannel,
			ServerSocketChannel apiConnectorChannel, String bpeBaseUrl, String fhirBaseUrl) throws Exception
	{
		Map<String, String> initParameters = new HashMap<>();
		initParameters.put("dev.dsf.server.status.port",
				Integer.toString(statusConnectorChannel.socket().getLocalPort()));

		initParameters.put("dev.dsf.bpe.db.url", "jdbc:postgresql://" + bpeLiquibaseRule.getHost() + ":"
				+ bpeLiquibaseRule.getMappedPort(5432) + "/" + bpeLiquibaseRule.getDatabaseName());
		initParameters.put("dev.dsf.bpe.db.user.group", BPE_DATABASE_USERS_GROUP);
		initParameters.put("dev.dsf.bpe.db.user.username", BPE_DATABASE_USER);
		initParameters.put("dev.dsf.bpe.db.user.password", BPE_DATABASE_USER_PASSWORD);
		initParameters.put("dev.dsf.bpe.db.user.engine.username", BPE_DATABASE_ENGINE_USER);
		initParameters.put("dev.dsf.bpe.db.user.engine.password", BPE_DATABASE_ENGINE_USER_PASSWORD);

		initParameters.put("dev.dsf.bpe.fhir.client.certificate", certificates.getClientCertificateFile().toString());
		initParameters.put("dev.dsf.bpe.fhir.client.certificate.private.key",
				certificates.getClientCertificatePrivateKeyFile().toString());
		initParameters.put("dev.dsf.bpe.fhir.client.certificate.private.key.password",
				String.valueOf(X509Certificates.PASSWORD));
		initParameters.put("dev.dsf.bpe.fhir.client.trust.server.certificate.cas",
				certificates.getCaCertificateFile().toString());
		initParameters.put("dev.dsf.bpe.mail.trust.server.certificate.cas",
				certificates.getCaCertificateFile().toString());
		initParameters.put("dev.dsf.server.auth.trust.client.certificate.cas",
				certificates.getCaCertificateFile().toString());

		initParameters.put("dev.dsf.bpe.server.base.url", bpeBaseUrl);
		initParameters.put("dev.dsf.bpe.fhir.server.base.url", fhirBaseUrl);

		initParameters.put("dev.dsf.bpe.process.api.directory", "../dsf-bpe-server-jetty/docker/api");
		initParameters.put("dev.dsf.bpe.process.plugin.directory", EMPTY_PROCESS_DIRECTORY.toString());
		initParameters.put("dev.dsf.bpe.process.plugin.exploded",
				"../dsf-bpe-test-plugin-v1/target/classes, ../dsf-bpe-test-plugin-v2/target/classes");

		initParameters.put("dev.dsf.bpe.process.api.allowed.bpe.classes",
				"{v1: '" + ALLOWED_BPE_CLASSES_LIST_FILE_V1 + "', v2: '" + ALLOWED_BPE_CLASSES_LIST_FILE_V2 + "'}");

		initParameters.put("dev.dsf.proxy.url", "http://proxy:8080");
		initParameters.put("dev.dsf.proxy.username", "proxy_username");
		initParameters.put("dev.dsf.proxy.password", "proxy_password");
		initParameters.put("dev.dsf.proxy.noProxy", "localhost, noproxy:443");

		final String fhirConnectionsYaml = """
				test-fhir-data-server:
				  base-url: '#[testFhirDataServerBaseUrl]'
				dsf-fhir-server:
				  base-url: '#[fhirBaseUrl]'
				  test-connection-on-startup: yes
				  enable-debug-logging: no
				  cert-auth:
				    private-key-file: '#[client.key]'
				    certificate-file: '#[client.crt]'
				    password: '#[password]'
				via-proxy:
				  base-url: 'http://via.proxy/fhir'
				dic-user:
				  base-url: '#[fhirBaseUrl]'
				  test-connection-on-startup: yes
				  enable-debug-logging: no
				  cert-auth:
				    private-key-file: '#[dic.client.key]'
				    certificate-file: '#[dic.client.crt]'
				    password: '#[password]'
				uac-user:
				  base-url: '#[fhirBaseUrl]'
				  test-connection-on-startup: yes
				  enable-debug-logging: no
				  cert-auth:
				    private-key-file: '#[uac.client.key]'
				    certificate-file: '#[uac.client.crt]'
				    password: '#[password]'
				"""
				.replaceAll(Pattern.quote("#[testFhirDataServerBaseUrl]"),
						Matcher.quoteReplacement("https://localhost:" + testFhirDataServer.getAddress().getPort()))
				.replaceAll(Pattern.quote("#[fhirBaseUrl]"), Matcher.quoteReplacement(fhirBaseUrl))
				.replaceAll(Pattern.quote("#[client.key]"),
						Matcher.quoteReplacement(certificates.getClientCertificatePrivateKeyFile().toString()))
				.replaceAll(Pattern.quote("#[client.crt]"),
						Matcher.quoteReplacement(certificates.getClientCertificateFile().toString()))
				.replaceAll(Pattern.quote("#[dic.client.key]"),
						Matcher.quoteReplacement(certificates.getDicUserClientCertificatePrivateKeyFile().toString()))
				.replaceAll(Pattern.quote("#[dic.client.crt]"),
						Matcher.quoteReplacement(certificates.getDicUserClientCertificateFile().toString()))
				.replaceAll(Pattern.quote("#[uac.client.key]"),
						Matcher.quoteReplacement(certificates.getUacUserClientCertificatePrivateKeyFile().toString()))
				.replaceAll(Pattern.quote("#[uac.client.crt]"),
						Matcher.quoteReplacement(certificates.getUacUserClientCertificateFile().toString()))
				.replaceAll(Pattern.quote("#[password]"),
						Matcher.quoteReplacement(String.valueOf(X509Certificates.PASSWORD)));
		initParameters.put("dev.dsf.bpe.fhir.client.connections.config", fhirConnectionsYaml);
		initParameters.put("dev.dsf.bpe.fhir.client.connections.config.default.trust.server.certificate.cas",
				certificates.getCaCertificateFile().toString());

		initParameters.put("dev.dsf.bpe.test.env.mandatory", "test-value");

		KeyStore clientCertificateTrustStore = KeyStoreCreator
				.jksForTrustedCertificates(certificates.getCaCertificate());
		KeyStore bpeServerCertificateKeyStore = certificates.getBpeServerCertificate().keyStore();

		Function<Server, ServerConnector> apiConnector = JettyServer.httpsConnector(apiConnectorChannel,
				clientCertificateTrustStore, bpeServerCertificateKeyStore,
				certificates.getBpeServerCertificate().keyStorePassword(), false);
		Function<Server, ServerConnector> statusConnector = JettyServer.statusConnector(statusConnectorChannel);
		List<Class<? extends ServletContainerInitializer>> servletContainerInitializers = Arrays
				.asList(JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class);

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

		JettyServer server = new JettyServer(apiConnector, statusConnector, "dsf-bpe-server", BPE_CONTEXT_PATH,
				servletContainerInitializers, initParameters, securityHandlerConfigurer);

		server.start();

		return server;
	}

	private static List<String> readListFile(Path file) throws IOException
	{
		return new ArrayList<>(Files.readAllLines(file));
	}

	private static void writeListFile(Path file, List<String> entries) throws IOException
	{
		Files.write(file, entries, StandardCharsets.UTF_8);
	}

	@AfterClass
	public static void afterClass() throws Exception
	{
		try
		{
			if (bpeServer != null)
			{
				logger.info("Stopping BPE Server ...");
				bpeServer.stop();
			}
		}
		catch (Exception e)
		{
			logger.error("Error while stopping BPE Server", e);
		}

		bpeDefaultDataSource.unwrap(BasicDataSource.class).close();

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

		fhirDefaultDataSource.unwrap(BasicDataSource.class).close();

		logger.info("Deleting files {} ...", FILES_TO_DELETE);
		FILES_TO_DELETE.forEach(AbstractIntegrationTest::deleteFile);

		logger.info("Deleting directories {} ...", DIRECTORIES_TO_DELETE);
		DIRECTORIES_TO_DELETE.forEach(AbstractIntegrationTest::deleteDirectory);
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

	private static void deleteDirectory(Path directory)
	{
		try
		{
			Files.walkFileTree(directory, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
				{
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
				{
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e)
		{
			logger.error("Error while deleting directory {}, error: {}", directory.toString(), e.toString());
		}
	}

	protected static AnnotationConfigWebApplicationContext getBpeSpringWebApplicationContext()
	{
		return (AnnotationConfigWebApplicationContext) WebApplicationContextUtils
				.getWebApplicationContext(bpeServer.getServletContext());
	}
}
