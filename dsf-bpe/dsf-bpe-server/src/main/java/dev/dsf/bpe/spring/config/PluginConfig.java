package dev.dsf.bpe.spring.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.plugin.BpmnProcessStateChangeService;
import dev.dsf.bpe.plugin.BpmnProcessStateChangeServiceImpl;
import dev.dsf.bpe.plugin.FhirResourceHandler;
import dev.dsf.bpe.plugin.FhirResourceHandlerImpl;
import dev.dsf.bpe.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.plugin.ProcessPluginFactory;
import dev.dsf.bpe.plugin.ProcessPluginLoader;
import dev.dsf.bpe.plugin.ProcessPluginLoaderImpl;
import dev.dsf.bpe.plugin.ProcessPluginManager;
import dev.dsf.bpe.plugin.ProcessPluginManagerImpl;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginApiImpl;
import dev.dsf.bpe.v1.ProcessPluginDefinition;
import dev.dsf.bpe.v1.activity.DefaultUserTaskListener;
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
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelperImpl;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelperImpl;

@Configuration
public class PluginConfig
{
	@Autowired
	private Environment environment;

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private FhirClientConfig fhirClientConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private MailConfig mailConfig;

	@Autowired
	private SerializerConfig serializerConfig;

	@Autowired
	private CamundaConfig camundaConfig;

	@Bean
	public ProcessPluginApi processPluginApiV1()
	{
		FhirWebserviceClientProvider clientProvider = new FhirWebserviceClientProviderImpl(
				fhirClientConfig.clientProvider());
		EndpointProvider endpointProvider = new EndpointProviderImpl(clientProvider,
				propertiesConfig.getServerBaseUrl());
		FhirContext fhirContext = fhirConfig.fhirContext();
		MailService mailService = new MailServiceImpl(mailConfig.mailService());
		ObjectMapper objectMapper = serializerConfig.objectMapper();
		OrganizationProvider organizationProvider = new OrganizationProviderImpl(clientProvider,
				propertiesConfig.getServerBaseUrl());

		ProcessAuthorizationHelper processAuthorizationHelper = new ProcessAuthorizationHelperImpl();
		QuestionnaireResponseHelper questionnaireResponseHelper = new QuestionnaireResponseHelperImpl(
				propertiesConfig.getServerBaseUrl());
		ReadAccessHelper readAccessHelper = new ReadAccessHelperImpl();
		TaskHelper taskHelper = new TaskHelperImpl(propertiesConfig.getServerBaseUrl());

		return new ProcessPluginApiImpl(endpointProvider, fhirContext, clientProvider, mailService, objectMapper,
				organizationProvider, processAuthorizationHelper, questionnaireResponseHelper, readAccessHelper,
				taskHelper);
	}

	@Bean
	public ProcessPluginFactory<ProcessPluginDefinition, DefaultUserTaskListener> processPluginFactoryV1()
	{
		return new ProcessPluginFactoryImpl(processPluginApiV1(), defaultUserTaskListenerV1());
	}

	@Bean
	public DefaultUserTaskListener defaultUserTaskListenerV1()
	{
		return new DefaultUserTaskListener(processPluginApiV1());
	}

	@Bean
	public ProcessPluginLoader processPluginLoader()
	{
		Path processPluginDirectoryPath = propertiesConfig.getProcessPluginDirectory();

		if (!Files.isDirectory(processPluginDirectoryPath))
			throw new RuntimeException(
					"Process plug in directory '" + processPluginDirectoryPath.toString() + "' not readable");

		return new ProcessPluginLoaderImpl(List.of(processPluginFactoryV1()), processPluginDirectoryPath,
				fhirConfig.fhirContext(), (ConfigurableEnvironment) environment);
	}

	@Bean
	public BpmnProcessStateChangeService bpmnProcessStateChangeService()
	{
		return new BpmnProcessStateChangeServiceImpl(camundaConfig.processEngine().getRepositoryService(),
				daoConfig.processStateDao(), ProcessIdAndVersion.fromStrings(propertiesConfig.getProcessExcluded()),
				ProcessIdAndVersion.fromStrings(propertiesConfig.getProcessRetired()));
	}

	@Bean
	public FhirResourceHandler fhirResourceHandler()
	{
		return new FhirResourceHandlerImpl(fhirClientConfig.clientProvider().getLocalWebserviceClient(),
				daoConfig.processPluginResourcesDao(), fhirConfig.fhirContext(),
				propertiesConfig.getFhirServerRequestMaxRetries(), propertiesConfig.getFhirServerRetryDelayMillis());
	}

	@Bean
	public ProcessPluginManager processPluginManager()
	{
		return new ProcessPluginManagerImpl(
				List.of(camundaConfig.delegateProvider(), camundaConfig.fallbackSerializerFactory()),
				processPluginLoader(), bpmnProcessStateChangeService(), fhirResourceHandler(),
				processPluginApiV1().getOrganizationProvider());
	}
}
