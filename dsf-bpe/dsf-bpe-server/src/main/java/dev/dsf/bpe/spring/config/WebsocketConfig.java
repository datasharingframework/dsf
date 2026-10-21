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
package dev.dsf.bpe.spring.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import dev.dsf.bpe.health.WebsocketHealthCheck;
import dev.dsf.bpe.subscription.ConcurrentSubscriptionHandlerFactory;
import dev.dsf.bpe.subscription.LocalFhirConnector;
import dev.dsf.bpe.subscription.LocalFhirConnectorImpl;
import dev.dsf.bpe.subscription.QuestionnaireResponseHandler;
import dev.dsf.bpe.subscription.QuestionnaireResponseSubscriptionHandlerFactory;
import dev.dsf.bpe.subscription.ResourceHandler;
import dev.dsf.bpe.subscription.SubscriptionHandlerFactory;
import dev.dsf.bpe.subscription.TaskHandler;
import dev.dsf.bpe.subscription.TaskSubscriptionHandlerFactory;
import dev.dsf.common.status.health.HealthCheck;

@Configuration
public class WebsocketConfig
{
	private static final Logger logger = LoggerFactory.getLogger(WebsocketConfig.class);

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private OperatonConfig operatonConfig;

	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private DsfClientConfig dsfClientConfig;

	@Autowired
	private PluginConfig pluginConfig;


	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ExecutorService resourceExecutor(String resource)
	{
		int corePoolSize = propertiesConfig.getProcessStartOrContinueThreads();

		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, corePoolSize, 30, TimeUnit.MINUTES,
				new LinkedBlockingQueue<>(),
				(_, _) -> logger.error("Unable to handle {} - execution rejected", resource));
		executor.allowCoreThreadTimeOut(true);

		return executor;
	}

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
	public ExecutorService connectExecutor(String threadNamePrefix)
	{
		return Executors.newSingleThreadExecutor(r -> new Thread(r, threadNamePrefix + "-websocket-connector"));
	}

	@Bean
	public ResourceHandler<Task> taskHandler()
	{
		return new TaskHandler(operatonConfig.processEngine().getRepositoryService(),
				pluginConfig.processPluginManager(), fhirConfig.fhirContext(),
				operatonConfig.processEngine().getRuntimeService(),
				dsfClientConfig.clientProvider().getWebserviceClient());
	}

	@Bean
	public SubscriptionHandlerFactory<Task> taskSubscriptionHandlerFactory()
	{
		return new ConcurrentSubscriptionHandlerFactory<>(
				new TaskSubscriptionHandlerFactory(taskHandler(), daoConfig.lastEventTimeDaoTask()),
				resourceExecutor("Task"));
	}

	@Bean
	public LocalFhirConnector taskFhirConnector()
	{
		return new LocalFhirConnectorImpl<>(Task.class, dsfClientConfig.clientProvider(),
				taskSubscriptionHandlerFactory(), fhirConfig.fhirContext(),
				propertiesConfig.getTaskSubscriptionSearchParameter(), propertiesConfig.getWebsocketRetrySleepMillis(),
				propertiesConfig.getWebsocketMaxRetries(), connectExecutor("task"));
	}

	@Bean
	public HealthCheck taskWebsocketHealthCheck()
	{
		return new WebsocketHealthCheck("Task", taskFhirConnector());
	}

	@Bean
	public ResourceHandler<QuestionnaireResponse> questionnaireResponseHandler()
	{
		return new QuestionnaireResponseHandler(operatonConfig.processEngine().getRepositoryService(),
				pluginConfig.processPluginManager(), fhirConfig.fhirContext(),
				operatonConfig.processEngine().getTaskService(),
				dsfClientConfig.clientProvider().getWebserviceClient());
	}

	@Bean
	public SubscriptionHandlerFactory<QuestionnaireResponse> questionnaireResponseSubscriptionHandlerFactory()
	{
		return new ConcurrentSubscriptionHandlerFactory<>(
				new QuestionnaireResponseSubscriptionHandlerFactory(questionnaireResponseHandler(),
						daoConfig.lastEventTimeDaoQuestionnaireResponse()),
				resourceExecutor("QuestionnaireResponse"));
	}

	@Bean
	public LocalFhirConnector questionnaireResponseFhirConnector()
	{
		return new LocalFhirConnectorImpl<>(QuestionnaireResponse.class, dsfClientConfig.clientProvider(),
				questionnaireResponseSubscriptionHandlerFactory(), fhirConfig.fhirContext(),
				propertiesConfig.getQuestionnaireResponseSubscriptionSearchParameter(),
				propertiesConfig.getWebsocketRetrySleepMillis(), propertiesConfig.getWebsocketMaxRetries(),
				connectExecutor("questionnaire-response"));
	}

	@Bean
	public HealthCheck questionnaireResponseWebsocketHealthCheck()
	{
		return new WebsocketHealthCheck("QuestionnaireResponse", questionnaireResponseFhirConnector());
	}
}
