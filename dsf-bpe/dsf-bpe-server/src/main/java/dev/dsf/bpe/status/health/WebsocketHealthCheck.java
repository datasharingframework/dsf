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
package dev.dsf.bpe.status.health;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.subscription.LocalFhirConnector;
import dev.dsf.common.status.health.HealthCheck;
import dev.dsf.common.status.health.HealthStatus;

public class WebsocketHealthCheck implements HealthCheck, InitializingBean
{
	private final String component;
	private final LocalFhirConnector connector;

	public WebsocketHealthCheck(String component, LocalFhirConnector connector)
	{
		this.component = component;
		this.connector = connector;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(component, "component");
		Objects.requireNonNull(connector, "connector");
	}

	@Override
	public HealthStatus check()
	{
		boolean connected = connector.isConnected();

		return new HealthStatus(component + " Websocket", connected, connected ? "Connected" : "Not connected");
	}
}
