package dev.dsf.bpe.v2.spring;

import java.util.Locale;
import java.util.function.Function;

import org.apache.tika.detect.Detector;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.HapiLocalizer;
import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.config.DsfClientConfig;
import dev.dsf.bpe.api.config.FhirClientConfigs;
import dev.dsf.bpe.api.listener.ListenerFactory;
import dev.dsf.bpe.api.listener.ListenerFactoryImpl;
import dev.dsf.bpe.api.service.BpeMailService;
import dev.dsf.bpe.api.service.BpeOidcClientProvider;
import dev.dsf.bpe.api.service.BuildInfoProvider;
import dev.dsf.bpe.v2.client.dsf.ReferenceCleaner;
import dev.dsf.bpe.v2.client.dsf.ReferenceCleanerImpl;
import dev.dsf.bpe.v2.client.dsf.ReferenceExtractor;
import dev.dsf.bpe.v2.client.dsf.ReferenceExtractorImpl;
import dev.dsf.bpe.v2.client.fhir.ClientConfigs;
import dev.dsf.bpe.v2.client.fhir.ClientConfigsDelegate;
import dev.dsf.bpe.v2.config.ProxyConfig;
import dev.dsf.bpe.v2.config.ProxyConfigDelegate;
import dev.dsf.bpe.v2.listener.ContinueListener;
import dev.dsf.bpe.v2.listener.EndListener;
import dev.dsf.bpe.v2.listener.ListenerVariables;
import dev.dsf.bpe.v2.listener.StartListener;
import dev.dsf.bpe.v2.plugin.ProcessPluginFactoryImpl;
import dev.dsf.bpe.v2.service.CryptoService;
import dev.dsf.bpe.v2.service.CryptoServiceImpl;
import dev.dsf.bpe.v2.service.DataLogger;
import dev.dsf.bpe.v2.service.DataLoggerImpl;
import dev.dsf.bpe.v2.service.DsfClientProvider;
import dev.dsf.bpe.v2.service.DsfClientProviderImpl;
import dev.dsf.bpe.v2.service.EndpointProvider;
import dev.dsf.bpe.v2.service.EndpointProviderImpl;
import dev.dsf.bpe.v2.service.FhirClientConfigProvider;
import dev.dsf.bpe.v2.service.FhirClientConfigProviderImpl;
import dev.dsf.bpe.v2.service.FhirClientConfigProviderWithEndpointSupport;
import dev.dsf.bpe.v2.service.FhirClientProvider;
import dev.dsf.bpe.v2.service.FhirClientProviderImpl;
import dev.dsf.bpe.v2.service.MailService;
import dev.dsf.bpe.v2.service.MailServiceDelegate;
import dev.dsf.bpe.v2.service.MimeTypeService;
import dev.dsf.bpe.v2.service.MimeTypeServiceImpl;
import dev.dsf.bpe.v2.service.OidcClientProvider;
import dev.dsf.bpe.v2.service.OidcClientProviderDelegate;
import dev.dsf.bpe.v2.service.OrganizationProvider;
import dev.dsf.bpe.v2.service.OrganizationProviderImpl;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelper;
import dev.dsf.bpe.v2.service.QuestionnaireResponseHelperImpl;
import dev.dsf.bpe.v2.service.ReadAccessHelper;
import dev.dsf.bpe.v2.service.ReadAccessHelperImpl;
import dev.dsf.bpe.v2.service.TargetProvider;
import dev.dsf.bpe.v2.service.TargetProviderImpl;
import dev.dsf.bpe.v2.service.TaskHelper;
import dev.dsf.bpe.v2.service.TaskHelperImpl;
import dev.dsf.bpe.v2.service.detector.CombinedDetectors;
import dev.dsf.bpe.v2.service.detector.NdJsonDetector;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelper;
import dev.dsf.bpe.v2.service.process.ProcessAuthorizationHelperImpl;
import dev.dsf.bpe.v2.variables.FhirResourceSerializer;
import dev.dsf.bpe.v2.variables.FhirResourcesListSerializer;
import dev.dsf.bpe.v2.variables.JsonHolderSerializer;
import dev.dsf.bpe.v2.variables.ObjectMapperFactory;
import dev.dsf.bpe.v2.variables.TargetSerializer;
import dev.dsf.bpe.v2.variables.TargetsSerializer;
import dev.dsf.bpe.v2.variables.VariablesImpl;

@Configuration
public class ApiServiceConfig
{
	@Autowired
	private DsfClientConfig dsfClientConfig;

	@Autowired
	private FhirClientConfigs fhirClientConfigs;

	@Autowired
	private BpeProxyConfig proxyConfig;

	@Autowired
	private BuildInfoProvider buildInfoProvider;

	@Autowired
	private BpeMailService bpeMailService;

	@Autowired
	private BpeOidcClientProvider bpeOidcClientProvider;

	@Bean
	public ProxyConfig proxyConfigDelegate()
	{
		return new ProxyConfigDelegate(proxyConfig);
	}

	@Bean
	public EndpointProvider endpointProvider()
	{
		return new EndpointProviderImpl(dsfClientProvider(), dsfClientConfig.getLocalConfig().getBaseUrl());
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
	public DsfClientProvider dsfClientProvider()
	{
		return new DsfClientProviderImpl(fhirContext(), referenceCleaner(), dsfClientConfig, proxyConfig,
				buildInfoProvider);
	}

	@Bean
	public FhirClientProvider fhirClientProvider()
	{
		return new FhirClientProviderImpl(fhirContext(), proxyConfigDelegate(), oidcClientProvider(),
				buildInfoProvider.getUserAgentValue(), fhirClientConfigProvider());
	}

	@Bean
	public FhirClientConfigProvider fhirClientConfigProvider()
	{
		return new FhirClientConfigProviderWithEndpointSupport(endpointProvider(),
				new FhirClientConfigProviderImpl(fhirClientConfigs.defaultTrustStore(), clientConfigsDelegate()));
	}

	@Bean
	public OidcClientProvider oidcClientProvider()
	{
		return new OidcClientProviderDelegate(bpeOidcClientProvider);
	}

	@Bean
	public MailService mailService()
	{
		return new MailServiceDelegate(bpeMailService);
	}

	@Bean
	public MimeTypeService mimeTypeService()
	{
		Detector detector = CombinedDetectors.withDefaultAndNdJson(NdJsonDetector.DEFAULT_LINES_TO_CHECK);
		return new MimeTypeServiceImpl(detector);
	}

	@Bean
	public ObjectMapper objectMapper()
	{
		return ObjectMapperFactory.createObjectMapper(fhirContext());
	}

	@Bean
	public OrganizationProvider organizationProvider()
	{
		return new OrganizationProviderImpl(dsfClientProvider(), dsfClientConfig.getLocalConfig().getBaseUrl());
	}

	@Bean
	public ProcessAuthorizationHelper processAuthorizationHelper()
	{
		return new ProcessAuthorizationHelperImpl();
	}

	@Bean
	public QuestionnaireResponseHelper questionnaireResponseHelper()
	{
		return new QuestionnaireResponseHelperImpl(dsfClientConfig.getLocalConfig().getBaseUrl());
	}

	@Bean
	public ReadAccessHelper readAccessHelper()
	{
		return new ReadAccessHelperImpl();
	}

	@Bean
	public TaskHelper taskHelper()
	{
		return new TaskHelperImpl(dsfClientConfig.getLocalConfig().getBaseUrl());
	}

	@Bean
	public ReferenceCleaner referenceCleaner()
	{
		return new ReferenceCleanerImpl(referenceExtractor());
	}

	@Bean
	public ClientConfigs clientConfigsDelegate()
	{
		return new ClientConfigsDelegate(fhirClientConfigs, proxyConfig);
	}

	@Bean
	public ReferenceExtractor referenceExtractor()
	{
		return new ReferenceExtractorImpl();
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
	public JsonHolderSerializer jsonVariableSerializer()
	{
		return new JsonHolderSerializer();
	}

	@Bean
	public Function<DelegateExecution, ListenerVariables> listenerVariablesFactory()
	{
		return execution -> new VariablesImpl(execution, objectMapper());
	}

	@Bean
	public ExecutionListener startListener()
	{
		return new StartListener(dsfClientConfig.getLocalConfig().getBaseUrl(), listenerVariablesFactory());
	}

	@Bean
	public ExecutionListener endListener()
	{
		return new EndListener(dsfClientConfig.getLocalConfig().getBaseUrl(), listenerVariablesFactory(),
				dsfClientProvider().getLocalDsfClient());
	}

	@Bean
	public ExecutionListener continueListener()
	{
		return new ContinueListener(dsfClientConfig.getLocalConfig().getBaseUrl(), listenerVariablesFactory());
	}

	@Bean
	public ListenerFactory listenerFactory()
	{
		return new ListenerFactoryImpl(ProcessPluginFactoryImpl.API_VERSION, startListener(), endListener(),
				continueListener());
	}

	@Bean
	public CryptoService cryptoService()
	{
		return new CryptoServiceImpl();
	}

	@Bean
	public TargetProvider targetProvider()
	{
		return new TargetProviderImpl(dsfClientProvider(), dsfClientConfig.getLocalConfig().getBaseUrl());
	}

	@Bean
	public DataLogger dataLogger()
	{
		return new DataLoggerImpl(fhirContext());
	}
}
