package dev.dsf.bpe.v2.spring;

import java.util.Locale;
import java.util.UUID;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.HapiLocalizer;
import dev.dsf.bpe.api.config.ClientConfig;
import dev.dsf.bpe.api.listener.ListenerFactory;
import dev.dsf.bpe.api.listener.ListenerFactoryImpl;
import dev.dsf.bpe.api.service.BpeMailService;
import dev.dsf.bpe.api.service.BuildInfoProvider;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.ProcessPluginApiImpl;
import dev.dsf.bpe.v2.client.ReferenceCleaner;
import dev.dsf.bpe.v2.client.ReferenceCleanerImpl;
import dev.dsf.bpe.v2.client.ReferenceExtractor;
import dev.dsf.bpe.v2.client.ReferenceExtractorImpl;
import dev.dsf.bpe.v2.config.ProxyConfig;
import dev.dsf.bpe.v2.config.ProxyConfigDelegate;
import dev.dsf.bpe.v2.listener.ContinueListener;
import dev.dsf.bpe.v2.listener.EndListener;
import dev.dsf.bpe.v2.listener.StartListener;
import dev.dsf.bpe.v2.plugin.ProcessPluginFactoryImpl;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.EndpointProviderImpl;
import dev.dsf.bpe.v2.service.FhirWebserviceClientProvider;
import dev.dsf.bpe.v2.service.FhirWebserviceClientProviderImpl;
import dev.dsf.bpe.v2.service.MailService;
import dev.dsf.bpe.v2.service.MailServiceImpl;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.OrganizationProviderImpl;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelperImpl;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.ReadAccessHelperImpl;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.service.TaskHelperImpl;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelper;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelperImpl;
import dev.dsf.bpe.v2.variables.FhirResourceSerializer;
import dev.dsf.bpe.v2.variables.FhirResourcesListSerializer;
import dev.dsf.bpe.v2.variables.ObjectMapperFactory;
import dev.dsf.bpe.v2.variables.TargetSerializer;
import dev.dsf.bpe.v2.variables.TargetsSerializer;
import dev.dsf.bpe.v2.variables.VariablesImpl;

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
	public ReferenceExtractor referenceExtractor()
	{
		return new ReferenceExtractorImpl();
	}

	@Bean
	public ReferenceCleaner referenceCleaner()
	{
		return new ReferenceCleanerImpl(referenceExtractor());
	}

	@Bean
	public FhirWebserviceClientProvider clientProvider()
	{
		char[] keyStorePassword = UUID.randomUUID().toString().toCharArray();

		return new FhirWebserviceClientProviderImpl(fhirContext(), environmentConfig.getFhirServerBaseUrl(),
				environmentConfig.getWebserviceClientLocalReadTimeout(),
				environmentConfig.getWebserviceClientLocalConnectTimeout(),
				environmentConfig.getWebserviceClientLocalVerbose(), environmentConfig.getWebserviceTrustStore(),
				environmentConfig.getWebserviceKeyStore(keyStorePassword), keyStorePassword,
				environmentConfig.getWebserviceClientRemoteReadTimeout(),
				environmentConfig.getWebserviceClientRemoteConnectTimeout(),
				environmentConfig.getWebserviceClientRemoteVerbose(), this.proxyConfig, buildInfoProvider,
				referenceCleaner());
	}

	@Bean
	public FhirContext fhirContext()
	{
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
