package dev.dsf.bpe.spring.config;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.subscription.ConcurrentSubscriptionHandlerFactory;
import dev.dsf.bpe.subscription.FhirConnector;
import dev.dsf.bpe.subscription.FhirConnectorImpl;
import dev.dsf.bpe.subscription.QuestionnaireResponseHandler;
import dev.dsf.bpe.subscription.QuestionnaireResponseSubscriptionHandlerFactory;
import dev.dsf.bpe.subscription.ResourceHandler;
import dev.dsf.bpe.subscription.SubscriptionHandlerFactory;
import dev.dsf.bpe.subscription.TaskHandler;
import dev.dsf.bpe.subscription.TaskSubscriptionHandlerFactory;

@Configuration
public class WebsocketConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private CamundaConfig camundaConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private FhirClientConfig fhirClientConfig;

	@Bean
	public ResourceHandler<Task> taskHandler()
	{
		return new TaskHandler(camundaConfig.processEngine().getRuntimeService(),
				camundaConfig.processEngine().getRepositoryService(),
				fhirClientConfig.clientProvider().getLocalWebserviceClient());
	}

	@Bean
	public SubscriptionHandlerFactory<Task> taskSubscriptionHandlerFactory()
	{
		return new ConcurrentSubscriptionHandlerFactory<>(propertiesConfig.getProcessStartOrContinueThreads(),
				new TaskSubscriptionHandlerFactory(taskHandler(), daoConfig.lastEventTimeDaoTask()));
	}

	@Bean
	public FhirConnector fhirConnectorTask()
	{
		return new FhirConnectorImpl<>("Task", fhirClientConfig.clientProvider(), taskSubscriptionHandlerFactory(),
				fhirConfig.fhirContext(), propertiesConfig.getTaskSubscriptionSearchParameter(),
				propertiesConfig.getWebsocketRetrySleepMillis(), propertiesConfig.getWebsocketMaxRetries());
	}

	@Bean
	public ResourceHandler<QuestionnaireResponse> questionnaireResponseHandler()
	{
		return new QuestionnaireResponseHandler(camundaConfig.processEngine().getTaskService());
	}

	@Bean
	public SubscriptionHandlerFactory<QuestionnaireResponse> questionnaireResponseSubscriptionHandlerFactory()
	{
		return new ConcurrentSubscriptionHandlerFactory<>(propertiesConfig.getProcessStartOrContinueThreads(),
				new QuestionnaireResponseSubscriptionHandlerFactory(questionnaireResponseHandler(),
						daoConfig.lastEventTimeDaoQuestionnaireResponse()));
	}

	@Bean
	public FhirConnector fhirConnectorQuestionnaireResponse()
	{
		return new FhirConnectorImpl<>("QuestionnaireResponse", fhirClientConfig.clientProvider(),
				questionnaireResponseSubscriptionHandlerFactory(), fhirConfig.fhirContext(),
				propertiesConfig.getQuestionnaireResponseSubscriptionSearchParameter(),
				propertiesConfig.getWebsocketRetrySleepMillis(), propertiesConfig.getWebsocketMaxRetries());
	}
}
