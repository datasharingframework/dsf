package dev.dsf.bpe.spring.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pkcs.PKCSException;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import ca.uhn.fhir.context.FhirContext;
import de.rwh.utils.crypto.CertificateHelper;
import de.rwh.utils.crypto.io.CertificateReader;
import de.rwh.utils.crypto.io.PemIo;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.client.FhirClientProviderImpl;
import dev.dsf.fhir.client.FhirWebsocketClientProvider;
import dev.dsf.fhir.endpoint.EndpointProviderImpl;
import dev.dsf.fhir.organization.EndpointProvider;
import dev.dsf.fhir.organization.OrganizationProvider;
import dev.dsf.fhir.organization.OrganizationProviderImpl;
import dev.dsf.fhir.questionnaire.QuestionnaireResponseHandler;
import dev.dsf.fhir.questionnaire.QuestionnaireResponseHelper;
import dev.dsf.fhir.questionnaire.QuestionnaireResponseHelperImpl;
import dev.dsf.fhir.questionnaire.QuestionnaireResponseSubscriptionHandlerFactory;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceExtractorImpl;
import dev.dsf.fhir.subscription.SubscriptionHandlerFactory;
import dev.dsf.fhir.task.TaskHandler;
import dev.dsf.fhir.task.TaskHelper;
import dev.dsf.fhir.task.TaskHelperImpl;
import dev.dsf.fhir.task.TaskSubscriptionHandlerFactory;
import dev.dsf.fhir.websocket.FhirConnector;
import dev.dsf.fhir.websocket.FhirConnectorImpl;
import dev.dsf.fhir.websocket.ResourceHandler;

@Configuration
public class FhirConfig implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(FhirConfig.class);

	private static final BouncyCastleProvider provider = new BouncyCastleProvider();

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private CamundaConfig camundaConfig;

	@Bean
	public FhirContext fhirContext()
	{
		return FhirContext.forR4();
	}

	@Bean
	public ReferenceCleaner referenceCleaner()
	{
		return new ReferenceCleanerImpl(referenceExtractor());
	}

	@Bean
	public ReferenceExtractor referenceExtractor()
	{
		return new ReferenceExtractorImpl();
	}

	@Bean
	public FhirWebsocketClientProvider clientProvider()
	{
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

		try
		{
			KeyStore webserviceKeyStore = createKeyStore(propertiesConfig.getClientCertificateFile(),
					propertiesConfig.getClientCertificatePrivateKeyFile(),
					propertiesConfig.getClientCertificatePrivateKeyFilePassword(), keyStorePassword);
			KeyStore webserviceTrustStore = createTrustStore(propertiesConfig.getClientCertificateTrustStoreFile());

			return new FhirClientProviderImpl(fhirContext(), referenceCleaner(), propertiesConfig.getServerBaseUrl(),
					propertiesConfig.getWebserviceClientLocalReadTimeout(),
					propertiesConfig.getWebserviceClientLocalConnectTimeout(),
					propertiesConfig.getWebserviceClientLocalProxySchemeHostPort(),
					propertiesConfig.getWebserviceClientLocalProxyUsername(),
					propertiesConfig.getWebserviceClientLocalProxyPassword(),
					propertiesConfig.getWebserviceClientLocalVerbose(), webserviceTrustStore, webserviceKeyStore,
					keyStorePassword, propertiesConfig.getWebserviceClientRemoteReadTimeout(),
					propertiesConfig.getWebserviceClientRemoteConnectTimeout(),
					propertiesConfig.getWebserviceClientRemoteProxySchemeHostPort(),
					propertiesConfig.getWebserviceClientRemoteProxyUsername(),
					propertiesConfig.getWebserviceClientRemoteProxyPassword(),
					propertiesConfig.getWebserviceClientRemoteVerbose(), getWebsocketUrl(), webserviceTrustStore,
					webserviceKeyStore, keyStorePassword, propertiesConfig.getWebsocketClientProxySchemeHostPort(),
					propertiesConfig.getWebsocketClientProxyUsername(),
					propertiesConfig.getWebsocketClientProxyPassword());
		}
		catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | PKCSException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String getWebsocketUrl()
	{
		String baseUrl = propertiesConfig.getServerBaseUrl();

		if (baseUrl.startsWith("https://"))
			return baseUrl.replace("https://", "wss://") + "/ws";
		else if (baseUrl.startsWith("http://"))
			return baseUrl.replace("http://", "ws://") + "/ws";
		else
			throw new RuntimeException("server base url (" + baseUrl + ") does not start with https:// or http://");
	}

	private KeyStore createTrustStore(String trustStoreFile)
			throws IOException, NoSuchAlgorithmException, CertificateException, KeyStoreException
	{
		Path trustStorePath = Paths.get(trustStoreFile);

		if (!Files.isReadable(trustStorePath))
			throw new IOException("Trust store file '" + trustStorePath.toString() + "' not readable");

		return CertificateReader.allFromCer(trustStorePath);
	}

	private KeyStore createKeyStore(String certificateFile, String privateKeyFile, char[] privateKeyPassword,
			char[] keyStorePassword)
			throws IOException, PKCSException, CertificateException, KeyStoreException, NoSuchAlgorithmException
	{
		Path certificatePath = Paths.get(certificateFile);
		Path privateKeyPath = Paths.get(privateKeyFile);

		if (!Files.isReadable(certificatePath))
			throw new IOException("Certificate file '" + certificatePath.toString() + "' not readable");
		if (!Files.isReadable(certificatePath))
			throw new IOException("Private key file '" + privateKeyPath.toString() + "' not readable");

		X509Certificate certificate = PemIo.readX509CertificateFromPem(certificatePath);
		PrivateKey privateKey = PemIo.readPrivateKeyFromPem(provider, privateKeyPath, privateKeyPassword);

		String subjectCommonName = CertificateHelper.getSubjectCommonName(certificate);
		return CertificateHelper.toJksKeyStore(privateKey, new Certificate[] { certificate }, subjectCommonName,
				keyStorePassword);
	}

	@Bean
	public OrganizationProvider organizationProvider()
	{
		return new OrganizationProviderImpl(clientProvider(), propertiesConfig.getOrganizationIdentifierValue());
	}

	@Bean
	public EndpointProvider endpointProvider()
	{
		return new EndpointProviderImpl(clientProvider(), propertiesConfig.getOrganizationIdentifierValue());
	}

	@Bean
	public ResourceHandler<Task> taskHandler()
	{
		return new TaskHandler(camundaConfig.processEngine().getRuntimeService(),
				camundaConfig.processEngine().getRepositoryService(), clientProvider().getLocalWebserviceClient(),
				taskHelper());
	}

	@Bean
	public SubscriptionHandlerFactory<Task> taskSubscriptionHandlerFactory()
	{
		return new TaskSubscriptionHandlerFactory(taskHandler(), daoConfig.lastEventTimeDaoTask());
	}

	@Bean
	public FhirConnector fhirConnectorTask()
	{
		return new FhirConnectorImpl<>("Task", clientProvider(), taskSubscriptionHandlerFactory(), fhirContext(),
				propertiesConfig.getTaskSubscriptionSearchParameter(), propertiesConfig.getWebsocketRetrySleepMillis(),
				propertiesConfig.getWebsocketMaxRetries());
	}

	@Bean
	public ResourceHandler<QuestionnaireResponse> questionnaireResponseHandler()
	{
		return new QuestionnaireResponseHandler(camundaConfig.processEngine().getTaskService());
	}

	@Bean
	public SubscriptionHandlerFactory<QuestionnaireResponse> questionnaireResponseSubscriptionHandlerFactory()
	{
		return new QuestionnaireResponseSubscriptionHandlerFactory(questionnaireResponseHandler(),
				daoConfig.lastEventTimeDaoQuestionnaireResponse());
	}

	@Bean
	public FhirConnector fhirConnectorQuestionnaireResponse()
	{
		return new FhirConnectorImpl<>("QuestionnaireResponse", clientProvider(),
				questionnaireResponseSubscriptionHandlerFactory(), fhirContext(),
				propertiesConfig.getQuestionnaireResponseSubscriptionSearchParameter(),
				propertiesConfig.getWebsocketRetrySleepMillis(), propertiesConfig.getWebsocketMaxRetries());
	}

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent(ContextRefreshedEvent event)
	{
		fhirConnectorTask().connect();
		fhirConnectorQuestionnaireResponse().connect();
	}

	@Bean
	public TaskHelper taskHelper()
	{
		return new TaskHelperImpl();
	}

	@Bean
	public QuestionnaireResponseHelper questionnaireResponseHelper()
	{
		return new QuestionnaireResponseHelperImpl();
	}

	@Bean
	public ReadAccessHelper readAccessHelper()
	{
		return new ReadAccessHelperImpl();
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		logger.info(
				"Local webservice client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " proxyUrl {}, proxyUsername {}, proxyPassword {}, serverBase: {}}",
				propertiesConfig.getClientCertificateTrustStoreFile(), propertiesConfig.getClientCertificateFile(),
				propertiesConfig.getClientCertificatePrivateKeyFile(),
				propertiesConfig.getClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.getWebserviceClientLocalProxySchemeHostPort(),
				propertiesConfig.getWebserviceClientLocalProxyUsername(),
				propertiesConfig.getWebserviceClientLocalProxyPassword() != null ? "***" : "null",
				propertiesConfig.getServerBaseUrl());
		logger.info(
				"Local websocket client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " proxyUrl {}, proxyUsername {}, proxyPassword {}, websocketUrl: {}}",
				propertiesConfig.getClientCertificateTrustStoreFile(), propertiesConfig.getClientCertificateFile(),
				propertiesConfig.getClientCertificatePrivateKeyFile(),
				propertiesConfig.getClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.getWebsocketClientProxySchemeHostPort(),
				propertiesConfig.getWebsocketClientProxyUsername(),
				propertiesConfig.getWebsocketClientProxyPassword() != null ? "***" : "null", getWebsocketUrl());
		logger.info(
				"Remote webservice client config: {trustStorePath: {}, certificatePath: {}, privateKeyPath: {}, privateKeyPassword: {},"
						+ " proxyUrl {}, proxyUsername {}, proxyPassword {}}",
				propertiesConfig.getClientCertificateTrustStoreFile(), propertiesConfig.getClientCertificateFile(),
				propertiesConfig.getClientCertificatePrivateKeyFile(),
				propertiesConfig.getClientCertificatePrivateKeyFilePassword() != null ? "***" : "null",
				propertiesConfig.getWebserviceClientRemoteProxySchemeHostPort(),
				propertiesConfig.getWebserviceClientRemoteProxyUsername(),
				propertiesConfig.getWebserviceClientRemoteProxyPassword() != null ? "***" : "null");
	}
}
