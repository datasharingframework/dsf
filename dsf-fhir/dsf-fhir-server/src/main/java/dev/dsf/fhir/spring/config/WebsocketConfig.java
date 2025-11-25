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
package dev.dsf.fhir.spring.config;

import org.eclipse.jetty.ee10.websocket.jakarta.server.config.ContainerDefaultConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import dev.dsf.fhir.websocket.ServerEndpoint;
import dev.dsf.fhir.websocket.ServerEndpointRegistrationForAuthentication;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;

@Configuration
public class WebsocketConfig
{
	@Autowired
	private EventConfig eventConfig;

	@Bean
	public ServerEndpoint subscriptionEndpoint()
	{
		return new ServerEndpoint(eventConfig.webSocketSubscriptionManager());
	}

	@Bean
	public ServerEndpointRegistration subscriptionEndpointRegistration()
	{
		return new ServerEndpointRegistrationForAuthentication(containerDefaultConfigurator(), ServerEndpoint.PATH,
				subscriptionEndpoint());
	}

	@Bean
	public ServerEndpointExporter endpointExporter()
	{
		return new ServerEndpointExporter();
	}

	@Bean
	public Configurator containerDefaultConfigurator()
	{
		return new ContainerDefaultConfigurator();
	}
}
