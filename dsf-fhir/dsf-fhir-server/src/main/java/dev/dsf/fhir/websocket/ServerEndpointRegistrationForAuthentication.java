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
package dev.dsf.fhir.websocket;

import java.util.Objects;

import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import jakarta.websocket.Endpoint;

public class ServerEndpointRegistrationForAuthentication extends ServerEndpointRegistration
{
	private final Configurator containerDefaultConfigurator;

	/**
	 * @param path
	 *            not <code>null</code>, not blank
	 * @param endpoint
	 *            not <code>null</code>
	 * @param containerDefaultConfigurator
	 *            not <code>null</code>
	 */
	public ServerEndpointRegistrationForAuthentication(String path, Endpoint endpoint,
			Configurator containerDefaultConfigurator)
	{
		super(path, endpoint);

		this.containerDefaultConfigurator = Objects.requireNonNull(containerDefaultConfigurator,
				"containerDefaultConfigurator");
	}

	@Override
	public Configurator getContainerDefaultConfigurator()
	{
		return containerDefaultConfigurator;
	}
}
