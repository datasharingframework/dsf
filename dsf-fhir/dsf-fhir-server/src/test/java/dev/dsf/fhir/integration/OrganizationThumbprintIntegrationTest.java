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
package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;

import java.nio.channels.ServerSocketChannel;
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
import org.hl7.fhir.r4.model.Organization;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.SpringServletContainerInitializer;
import org.testcontainers.utility.DockerImageName;

import ca.uhn.fhir.context.FhirContext;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreCreator;
import de.hsheilbronn.mi.utils.test.PostgreSqlContainerLiquibaseTemplateClassRule;
import de.hsheilbronn.mi.utils.test.PostgresTemplateRule;
import dev.dsf.common.auth.ClientCertificateAuthenticator;
import dev.dsf.common.auth.DelegatingAuthenticator;
import dev.dsf.common.auth.DsfLoginService;
import dev.dsf.common.auth.DsfSecurityHandler;
import dev.dsf.common.auth.StatusPortAuthenticator;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.FhirWebserviceClientJersey;
import dev.dsf.fhir.dao.AbstractDbTest;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractorImpl;
import jakarta.servlet.ServletContainerInitializer;

public class OrganizationThumbprintIntegrationTest extends AbstractDbTest
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

	private static final ReferenceCleaner referenceCleaner = new ReferenceCleanerImpl(new ReferenceExtractorImpl());

	private static String baseUrl;
	private static JettyServer fhirServer;
	private static FhirWebserviceClient webserviceClient;
	private ServerSocketChannel apiConnectorChannel;
	private ServerSocketChannel statusConnectorChannel;

	@BeforeClass
	public static void beforeClass() throws Exception
	{
		defaultDataSource = createDefaultDataSource(liquibaseRule.getHost(), liquibaseRule.getMappedPort(5432),
				liquibaseRule.getDatabaseName());
		defaultDataSource.unwrap(BasicDataSource.class).start();
	}

	@Before
	public void before()
	{
		apiConnectorChannel = JettyServer.serverSocketChannel("127.0.0.1");
		statusConnectorChannel = JettyServer.serverSocketChannel("127.0.0.1");

		baseUrl = "https://localhost:" + apiConnectorChannel.socket().getLocalPort() + CONTEXT_PATH;

		logger.info("Creating webservice client ...");
		webserviceClient = createWebserviceClient(apiConnectorChannel.socket().getLocalPort(),
				certificates.getClientCertificate().trustStore(), certificates.getClientCertificate().keyStore(),
				certificates.getClientCertificate().keyStorePassword());

		logger.info("Creating template database ...");
		liquibaseRule.createTemplateDatabase();
	}

	private static FhirWebserviceClient createWebserviceClient(int apiPort, KeyStore trustStore, KeyStore keyStore,
			char[] keyStorePassword)
	{
		return new FhirWebserviceClientJersey("https://localhost:" + apiPort + CONTEXT_PATH, trustStore, keyStore,
				keyStorePassword, null, null, null, null, Duration.ZERO, Duration.ZERO, false,
				"DSF Integration Test Client", fhirContext, referenceCleaner);
	}

	private static Map<String, String> getDefaultInitParameters(ServerSocketChannel statusConnectorChannel,
			ServerSocketChannel apiConnectorChannel)
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

		initParameters.put("dev.dsf.fhir.server.endpoint.address",
				"https://localhost:" + apiConnectorChannel.socket().getLocalPort() + "/fhir");

		initParameters.put("dev.dsf.fhir.server.endpoint.address.external", "https://localhost:80010/fhir");
		initParameters.put("dev.dsf.fhir.server.organization.thumbprint.external",
				certificates.getExternalClientCertificate().certificateSha512ThumbprintHex());
		return initParameters;
	}

	private static Map<String, String> getInitParametersWithThumbprint(ServerSocketChannel statusConnectorChannel,
			ServerSocketChannel apiConnectorChannel)
	{
		Map<String, String> initParameters = getDefaultInitParameters(statusConnectorChannel, apiConnectorChannel);
		initParameters.put("dev.dsf.fhir.server.organization.thumbprint",
				certificates.getClientCertificate().certificateSha512ThumbprintHex());
		return initParameters;
	}

	private static JettyServer createFhirServer(Map<String, String> initParameters,
			ServerSocketChannel apiConnectorChannel, ServerSocketChannel statusConnectorChannel) throws Exception
	{

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

		return new JettyServer(apiConnector, statusConnector, "dsf-fhir-server", CONTEXT_PATH,
				servletContainerInitializers, initParameters, securityHandlerConfigurer);
	}

	@Test
	public void testFhirServerStartupWithoutThumbprint() throws Exception
	{
		fhirServer = createFhirServer(getDefaultInitParameters(statusConnectorChannel, apiConnectorChannel),
				apiConnectorChannel, statusConnectorChannel);
		fhirServer.start();
		Organization organization = (Organization) webserviceClient
				.search(Organization.class, Map.of("identifier", List.of("Test_Organization"))).getEntry().get(0)
				.getResource();
		String thumbprint = organization
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.getValue().primitiveValue();
		assertEquals(certificates.getClientCertificate().certificateSha512ThumbprintHex(), thumbprint);
	}

	@Test
	public void testFhirServerStartupWithThumbprint() throws Exception
	{
		fhirServer = createFhirServer(getInitParametersWithThumbprint(statusConnectorChannel, apiConnectorChannel),
				apiConnectorChannel, statusConnectorChannel);
		fhirServer.start();
		Organization organization = (Organization) webserviceClient
				.search(Organization.class, Map.of("identifier", List.of("Test_Organization"))).getEntry().get(0)
				.getResource();
		String thumbprint = organization
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.getValue().primitiveValue();
		assertEquals(certificates.getClientCertificate().certificateSha512ThumbprintHex(), thumbprint);
	}

	@After
	public void cleanUp() throws Exception
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

		try
		{
			if (apiConnectorChannel != null)
			{
				logger.info("Closing API Connector Channel...");
				apiConnectorChannel.close();
			}
		}
		catch (Exception e)
		{
			logger.error("Error while closing API Connector Channel", e);
		}

		try
		{
			if (statusConnectorChannel != null)
			{
				logger.info("Closing Status Connector Channel...");
				statusConnectorChannel.close();
			}
		}
		catch (Exception e)
		{
			logger.error("Error while closing Status Connector Channel", e);
		}

		if (webserviceClient != null)
		{
			webserviceClient = null;
		}
		defaultDataSource.unwrap(BasicDataSource.class).close();
	}
}
