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
package dev.dsf.bpe.v2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v2.client.fhir.ClientConfig;
import dev.dsf.bpe.v2.client.fhir.FhirClientFactory;

public class FhirClientProviderImpl implements FhirClientProvider
{
	private final FhirContext fhirContext;
	private final OidcClientProvider oidcClientProvider;
	private final String userAgent;
	private final ClientConfigProvider configProvider;

	private final Map<String, FhirClientFactory> factoriesById = new HashMap<>();

	/**
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param oidcClientProvider
	 *            not <code>null</code>
	 * @param userAgent
	 *            not <code>null</code>
	 * @param configProvider
	 *            not <code>null</code>
	 */
	public FhirClientProviderImpl(FhirContext fhirContext, OidcClientProvider oidcClientProvider, String userAgent,
			ClientConfigProvider configProvider)
	{
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.oidcClientProvider = Objects.requireNonNull(oidcClientProvider, "oidcClientProvider");
		this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
		this.configProvider = Objects.requireNonNull(configProvider, "configProvider");
	}

	private Optional<IGenericClient> doGetById(ClientConfig clientConfig)
	{
		if (clientConfig == null)
			return Optional.empty();

		synchronized (factoriesById)
		{
			FhirClientFactory factory = factoriesById.get(clientConfig.getFhirServerId());

			if (factory == null)
			{
				factory = createClientFactory(clientConfig);
				factoriesById.put(clientConfig.getFhirServerId(), factory);
			}

			return Optional.of(factory.newGenericClient(clientConfig.getBaseUrl()));
		}
	}

	@Override
	public Optional<IGenericClient> getById(String fhirServerId)
	{
		return configProvider.getClientConfig(fhirServerId).flatMap(this::doGetById);
	}

	protected FhirClientFactory createClientFactory(ClientConfig config)
	{
		return new FhirClientFactory(oidcClientProvider, config, fhirContext, userAgent);
	}
}
