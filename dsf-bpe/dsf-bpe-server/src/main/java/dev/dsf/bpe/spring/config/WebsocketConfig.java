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

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.status.health.WebsocketHealthCheck;
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
		return new ConcurrentSubscriptionHandlerFactory<>(propertiesConfig.getProcessStartOrContinueThreads(),
				new TaskSubscriptionHandlerFactory(taskHandler(), daoConfig.lastEventTimeDaoTask()));
	}

	@Bean
	public LocalFhirConnector taskFhirConnector()
	{
		return new LocalFhirConnectorImpl<>(Task.class, dsfClientConfig.clientProvider(),
				taskSubscriptionHandlerFactory(), fhirConfig.fhirContext(),
				propertiesConfig.getTaskSubscriptionSearchParameter(), propertiesConfig.getWebsocketRetrySleepMillis(),
				propertiesConfig.getWebsocketMaxRetries(), taskConnectExecutor());
	}

	@Bean
	public ExecutorService taskConnectExecutor()
	{
		return Executors.newSingleThreadExecutor(r -> new Thread(r, "task-websocket-connector"));
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
		return new ConcurrentSubscriptionHandlerFactory<>(propertiesConfig.getProcessStartOrContinueThreads(),
				new QuestionnaireResponseSubscriptionHandlerFactory(questionnaireResponseHandler(),
						daoConfig.lastEventTimeDaoQuestionnaireResponse()));
	}

	@Bean
	public LocalFhirConnector questionnaireResponseFhirConnector()
	{
		return new LocalFhirConnectorImpl<>(QuestionnaireResponse.class, dsfClientConfig.clientProvider(),
				questionnaireResponseSubscriptionHandlerFactory(), fhirConfig.fhirContext(),
				propertiesConfig.getQuestionnaireResponseSubscriptionSearchParameter(),
				propertiesConfig.getWebsocketRetrySleepMillis(), propertiesConfig.getWebsocketMaxRetries(),
				questionnaireResponseConnectExecutor());
	}

	@Bean
	public ExecutorService questionnaireResponseConnectExecutor()
	{
		return Executors.newSingleThreadExecutor(r -> new Thread(r, "questionnaire-response-websocket-connector"));
	}

	@Bean
	public HealthCheck questionnaireResponseWebsocketHealthCheck()
	{
		return new WebsocketHealthCheck("QuestionnaireResponse", questionnaireResponseFhirConnector());
	}
}
