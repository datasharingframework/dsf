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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
public class PostStartupConfig
{
	private static final Logger logger = LoggerFactory.getLogger(PostStartupConfig.class);

	@Autowired
	private PluginConfig pluginConfig;

	@Autowired
	private WebsocketConfig fhirConfig;

	@Autowired
	private OperatonConfig operatonConfig;

	@EventListener({ ContextRefreshedEvent.class })
	public void onContextRefreshedEvent()
	{
		logger.info("Deploying process plugins ...");
		pluginConfig.processPluginManager().loadAndDeployPlugins();
		logger.info("Deploying process plugins [Done]");

		logger.info("Starting process engine ...");
		operatonConfig.processEngineConfiguration().getJobExecutor().start();
		logger.info("Starting process engine [Done]");

		fhirConfig.fhirConnectorTask().connect();
		fhirConfig.fhirConnectorQuestionnaireResponse().connect();
		// websocket connect is an async operation
	}
}
