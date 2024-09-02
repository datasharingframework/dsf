package dev.dsf.bpe.v1.spring;

import java.security.KeyStore;
import java.util.Locale;
import java.util.UUID;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.HapiLocalizer;
import dev.dsf.bpe.api.config.ClientConfig;
import dev.dsf.bpe.api.listener.ListenerFactory;
import dev.dsf.bpe.api.listener.ListenerFactoryImpl;
import dev.dsf.bpe.api.service.BpeMailService;
import dev.dsf.bpe.api.service.BuildInfoProvider;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginApiImpl;
import dev.dsf.bpe.v1.config.ProxyConfig;
import dev.dsf.bpe.v1.config.ProxyConfigDelegate;
import dev.dsf.bpe.v1.listener.ContinueListener;
import dev.dsf.bpe.v1.listener.EndListener;
import dev.dsf.bpe.v1.listener.StartListener;
import dev.dsf.bpe.v1.plugin.ProcessPluginFactoryImpl;
import dev.dsf.bpe.v1.service.EndpointProvider;
import dev.dsf.bpe.v1.service.EndpointProviderImpl;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProviderImpl;
import dev.dsf.bpe.v1.service.MailService;
import dev.dsf.bpe.v1.service.MailServiceImpl;
import dev.dsf.bpe.v1.service.OrganizationProvider;
import dev.dsf.bpe.v1.service.OrganizationProviderImpl;
import dev.dsf.bpe.v1.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v1.service.QuestionnaireResponseHelperImpl;
import dev.dsf.bpe.v1.service.TaskHelper;
import dev.dsf.bpe.v1.service.TaskHelperImpl;
import dev.dsf.bpe.v1.variables.FhirResourceSerializer;
import dev.dsf.bpe.v1.variables.FhirResourcesListSerializer;
import dev.dsf.bpe.v1.variables.ObjectMapperFactory;
import dev.dsf.bpe.v1.variables.TargetSerializer;
import dev.dsf.bpe.v1.variables.TargetsSerializer;
import dev.dsf.bpe.v1.variables.VariablesImpl;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelperImpl;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceExtractorImpl;

@Configuration
public class ApiServiceConfig
{
	@Autowired
	private ClientConfig environmentConfig;

	@Autowired
	private dev.dsf.bpe.api.config.ProxyConfig proxyConfig;

	@Autowired
	private BuildInfoProvider buildInfoProvider;

	@Autowired
	private BpeMailService bpeMailService;

	@Bean
	public ProcessPluginApi processPluginApiV1()
	{
		ProxyConfig proxyConfig = new ProxyConfigDelegate(this.proxyConfig);

		FhirWebserviceClientProvider clientProvider = clientProvider();
		EndpointProvider endpointProvider = new EndpointProviderImpl(clientProvider,
				environmentConfig.getFhirServerBaseUrl());
		FhirContext fhirContext = fhirContext();
		MailService mailService = new MailServiceImpl(bpeMailService);
		ObjectMapper objectMapper = objectMapper();
		OrganizationProvider organizationProvider = new OrganizationProviderImpl(clientProvider,
				environmentConfig.getFhirServerBaseUrl());

		ProcessAuthorizationHelper processAuthorizationHelper = new ProcessAuthorizationHelperImpl();
		QuestionnaireResponseHelper questionnaireResponseHelper = new QuestionnaireResponseHelperImpl(
				environmentConfig.getFhirServerBaseUrl());
		ReadAccessHelper readAccessHelper = new ReadAccessHelperImpl();
		TaskHelper taskHelper = new TaskHelperImpl(environmentConfig.getFhirServerBaseUrl());

		return new ProcessPluginApiImpl(proxyConfig, endpointProvider, fhirContext, clientProvider, mailService,
				objectMapper, organizationProvider, processAuthorizationHelper, questionnaireResponseHelper,
				readAccessHelper, taskHelper);
	}

	@Bean
	public FhirWebserviceClientProvider clientProvider()
	{
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();
		KeyStore webserviceKeyStore = environmentConfig.getWebserviceKeyStore(keyStorePassword);
		KeyStore webserviceTrustStore = environmentConfig.getWebserviceTrustStore();

		return new FhirWebserviceClientProviderImpl(fhirContext(), referenceCleaner(),
				environmentConfig.getFhirServerBaseUrl(), environmentConfig.getWebserviceClientLocalReadTimeout(),
				environmentConfig.getWebserviceClientLocalConnectTimeout(),
				environmentConfig.getWebserviceClientLocalVerbose(), webserviceTrustStore, webserviceKeyStore,
				keyStorePassword, environmentConfig.getWebserviceClientRemoteReadTimeout(),
				environmentConfig.getWebserviceClientRemoteConnectTimeout(),
				environmentConfig.getWebserviceClientRemoteVerbose(), proxyConfig, buildInfoProvider);
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
	public FhirContext fhirContext()
	{
		// TODO remove workaround after upgrading to HAPI 6.8+, see https://github.com/hapifhir/hapi-fhir/issues/5205
		StreamReadConstraints.overrideDefaultStreamReadConstraints(
				StreamReadConstraints.builder().maxStringLength(Integer.MAX_VALUE).build());

		FhirContext context = FhirContext.forR4();
		HapiLocalizer localizer = new HapiLocalizer()
		{
			@Override
			public Locale getLocale()
			{
				return Locale.ROOT;
			}
		};
		context.setLocalizer(localizer);
		return context;
	}

	@Bean
	public ObjectMapper objectMapper()
	{
		return ObjectMapperFactory.createObjectMapper(fhirContext());
	}

	@Bean
	public FhirResourceSerializer fhirResourceSerializer()
	{
		return new FhirResourceSerializer(fhirContext());
	}

	@Bean
	public FhirResourcesListSerializer fhirResourcesListSerializer()
	{
		return new FhirResourcesListSerializer(objectMapper());
	}

	@Bean
	public TargetSerializer targetSerializer()
	{
		return new TargetSerializer(objectMapper());
	}

	@Bean
	public TargetsSerializer targetsSerializer()
	{
		return new TargetsSerializer(objectMapper());
	}

	@Bean
	public ExecutionListener startListener()
	{
		return new StartListener(environmentConfig.getFhirServerBaseUrl(), VariablesImpl::new);
	}

	@Bean
	public ExecutionListener endListener()
	{
		return new EndListener(environmentConfig.getFhirServerBaseUrl(), VariablesImpl::new,
				clientProvider().getLocalWebserviceClient());
	}

	@Bean
	public ExecutionListener continueListener()
	{
		return new ContinueListener(environmentConfig.getFhirServerBaseUrl(), VariablesImpl::new);
	}

	@Bean
	public ListenerFactory listenerFactory()
	{
		return new ListenerFactoryImpl(ProcessPluginFactoryImpl.API_VERSION, startListener(), endListener(),
				continueListener());
	}
}
