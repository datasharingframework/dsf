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

import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.config.DsfClientConfig;
import dev.dsf.bpe.api.config.DsfClientConfig.BaseConfig;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.client.dsf.DsfClientJersey;
import dev.dsf.bpe.v2.client.dsf.ReferenceCleaner;
import dev.dsf.bpe.v2.client.fhir.ClientConfig;

public class DsfClientProviderImpl implements DsfClientProvider, InitializingBean
{
	private final Map<String, DsfClient> clientsByUrlOrId = new HashMap<>();

	private final FhirContext fhirContext;
	private final ReferenceCleaner referenceCleaner;
	private final DsfClientConfig dsfClientConfig;
	private final BpeProxyConfig proxyConfig;
	private final OidcClientProvider oidcClientProvider;
	private final String userAgent;
	private final ClientConfigProvider configProvider;

	public DsfClientProviderImpl(FhirContext fhirContext, ReferenceCleaner referenceCleaner,
			DsfClientConfig dsfClientConfig, BpeProxyConfig proxyConfig, OidcClientProvider oidcClientProvider,
			String userAgent, ClientConfigProvider configProvider)
	{
		this.fhirContext = fhirContext;
		this.referenceCleaner = referenceCleaner;
		this.dsfClientConfig = dsfClientConfig;
		this.proxyConfig = proxyConfig;
		this.oidcClientProvider = oidcClientProvider;
		this.userAgent = userAgent;
		this.configProvider = configProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		Objects.requireNonNull(dsfClientConfig, "dsfClientConfig");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(oidcClientProvider, "oidcClientProvider");
		Objects.requireNonNull(userAgent, "userAgent");
		Objects.requireNonNull(configProvider, "configProvider");
	}

	@Override
	public Optional<DsfClient> getById(String fhirServerId)
	{
		return configProvider.getClientConfig(fhirServerId).flatMap(this::doGetById);
	}

	private Optional<DsfClient> doGetById(ClientConfig clientConfig)
	{
		if (clientConfig == null)
			return Optional.empty();

		synchronized (clientsByUrlOrId)
		{
			DsfClient client = clientsByUrlOrId.get(clientConfig.getFhirServerId());

			if (client == null)
			{
				client = new DsfClientJersey(clientConfig, oidcClientProvider, userAgent, fhirContext,
						referenceCleaner);
				clientsByUrlOrId.put(clientConfig.getFhirServerId(), client);
			}

			return Optional.of(client);
		}
	}

	@Override
	public DsfClient getLocal()
	{
		return getByEndpointUrl(dsfClientConfig.getLocalConfig().getBaseUrl());
	}

	@Override
	public DsfClient getByEndpointUrl(String webserviceUrl)
	{
		Objects.requireNonNull(webserviceUrl, "webserviceUrl");

		DsfClient cachedClient = clientsByUrlOrId.get(webserviceUrl);
		if (cachedClient != null)
			return cachedClient;
		else
		{
			DsfClient newClient = doGetByUrl(webserviceUrl);
			clientsByUrlOrId.put(webserviceUrl, newClient);
			return newClient;
		}
	}

	private DsfClient doGetByUrl(String webserviceUrl)
	{
		synchronized (clientsByUrlOrId)
		{
			if (clientsByUrlOrId.containsKey(webserviceUrl))
				return clientsByUrlOrId.get(webserviceUrl);

			String proxyHost = null, proxyUsername = null;
			char[] proxyPassword = null;
			if (proxyConfig.isEnabled(webserviceUrl))
			{
				proxyHost = proxyConfig.getUrl();
				proxyUsername = proxyConfig.getUsername();
				proxyPassword = proxyConfig.getPassword();
			}

			BaseConfig config = dsfClientConfig.getLocalConfig().getBaseUrl().equals(webserviceUrl)
					? dsfClientConfig.getLocalConfig()
					: dsfClientConfig.getRemoteConfig();

			DsfClient client = new DsfClientJersey(webserviceUrl, dsfClientConfig.getTrustStore(),
					dsfClientConfig.getKeyStore(), dsfClientConfig.getKeyStorePassword(), proxyHost, proxyUsername,
					proxyPassword, config.getConnectTimeout(), config.getReadTimeout(), config.isDebugLoggingEnabled(),
					userAgent, fhirContext, referenceCleaner);

			clientsByUrlOrId.put(webserviceUrl, client);

			return client;
		}
	}
}
