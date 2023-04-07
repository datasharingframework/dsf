package dev.dsf.bpe.spring.config;

import java.util.List;
import java.util.stream.Stream;

import org.camunda.bpm.engine.ProcessEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import dev.dsf.bpe.delegate.DelegateProvider;
import dev.dsf.bpe.plugin.ProcessPluginProvider;
import dev.dsf.bpe.process.BpmnFileAndModel;
import dev.dsf.bpe.process.ProcessKeyAndVersion;
import dev.dsf.bpe.process.ProcessStateChangeOutcome;
import dev.dsf.bpe.service.BpmnProcessStateChangeService;
import dev.dsf.bpe.service.BpmnProcessStateChangeServiceImpl;
import dev.dsf.bpe.service.BpmnServiceDelegateValidationService;
import dev.dsf.bpe.service.BpmnServiceDelegateValidationServiceImpl;
import dev.dsf.bpe.service.FhirResourceHandler;
import dev.dsf.bpe.service.FhirResourceHandlerImpl;

@Configuration
public class PostProcessDeployConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private ProcessEngine processEngine;

	@Autowired
	private DelegateProvider delegateProvider;

	@Autowired
	private ProcessPluginProvider processPluginProvider;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private DaoConfig daoConfig;

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent(ContextRefreshedEvent event)
	{
		Stream<BpmnFileAndModel> models = processPluginProvider.getDefinitions().stream()
				.flatMap(def -> def.getAndValidateModels().stream());

		List<ProcessStateChangeOutcome> changes = bpmnProcessStateChangeService()
				.deploySuspendOrActivateProcesses(models);

		bpmnServiceDelegateValidationService().validateModels();

		fhirResourceHandler().applyStateChangesAndStoreNewResourcesInDb(
				processPluginProvider.getDefinitionByProcessKeyAndVersion(), changes);

		processPluginProvider.onProcessesDeployed(changes);
	}

	@Bean
	public BpmnServiceDelegateValidationService bpmnServiceDelegateValidationService()
	{
		return new BpmnServiceDelegateValidationServiceImpl(processEngine, delegateProvider);
	}

	@Bean
	public BpmnProcessStateChangeService bpmnProcessStateChangeService()
	{
		return new BpmnProcessStateChangeServiceImpl(processEngine.getRepositoryService(), daoConfig.processStateDao(),
				processPluginProvider, ProcessKeyAndVersion.fromStrings(propertiesConfig.getProcessExcluded()),
				ProcessKeyAndVersion.fromStrings(propertiesConfig.getProcessRetired()));
	}

	@Bean
	public FhirResourceHandler fhirResourceHandler()
	{
		return new FhirResourceHandlerImpl(fhirConfig.clientProvider().getLocalWebserviceClient(),
				daoConfig.processPluginResourcesDao(), fhirConfig.fhirContext(),
				propertiesConfig.getFhirServerRequestMaxRetries(), propertiesConfig.getFhirServerRetryDelayMillis());
	}
}
