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
package dev.dsf.bpe.v2.client.fhir;

import java.util.List;

import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.config.FhirClientConfigs;

public class ClientConfigsDelegate implements ClientConfigs
{
	private final FhirClientConfigs delegate;
	private final BpeProxyConfig proxyConfig;

	public ClientConfigsDelegate(FhirClientConfigs delegate, BpeProxyConfig proxyConfig)
	{
		this.delegate = delegate;
		this.proxyConfig = proxyConfig;
	}

	@Override
	public List<ClientConfig> getConfigs()
	{
		return delegate.getConfigs().stream().map(d -> new ClientConfigDelegate(d, proxyConfig))
				.map(c -> (ClientConfig) c).toList();
	}
}
