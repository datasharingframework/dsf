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
package dev.dsf.bpe.v1.service;

import java.security.KeyStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.service.BuildInfoProvider;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.FhirWebserviceClientJersey;
import dev.dsf.fhir.service.ReferenceCleaner;

public class FhirWebserviceClientProviderImpl implements FhirWebserviceClientProvider
{
	private static final String USER_AGENT_VALUE = "DSF/";

	private final Map<String, FhirWebserviceClient> webserviceClientsByUrl = new HashMap<>();

	private final FhirContext fhirContext;
	private final ReferenceCleaner referenceCleaner;

	private final String baseUrlLocal;
	private final Duration readTimeoutLocal;
	private final Duration connectTimeoutLocal;
	private final boolean logRequestsAndResponsesLocal;

	private final KeyStore trustStore;
	private final KeyStore keyStore;
	private final char[] keyStorePassword;

	private final Duration readTimeoutRemote;
	private final Duration connectTimeoutRemote;
	private final boolean logRequestsAndResponsesRemote;

	private final BpeProxyConfig proxyConfig;
	private final BuildInfoProvider buildInfoProvider;

	/**
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param referenceCleaner
	 *            not <code>null</code>
	 * @param baseUrlLocal
	 *            not <code>null</code>
	 * @param readTimeoutLocal
	 *            not <code>null</code>, not negative, 0 represents infinity
	 * @param connectTimeoutLocal
	 *            not <code>null</code>, not negative, 0 represents infinity
	 * @param logRequestsAndResponsesLocal
	 * @param trustStore
	 *            not <code>null</code>
	 * @param keyStore
	 *            not <code>null</code>
	 * @param keyStorePassword
	 *            not <code>null</code>
	 * @param readTimeoutRemote
	 *            not <code>null</code>, not negative, 0 represents infinity
	 * @param connectTimeoutRemote
	 *            not <code>null</code>, not negative, 0 represents infinity
	 * @param logRequestsAndResponsesRemote
	 * @param proxyConfig
	 *            not <code>null</code>
	 * @param buildInfoProvider
	 *            not <code>null</code>
	 */
	public FhirWebserviceClientProviderImpl(FhirContext fhirContext, ReferenceCleaner referenceCleaner,
			String baseUrlLocal, Duration readTimeoutLocal, Duration connectTimeoutLocal,
			boolean logRequestsAndResponsesLocal, KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword,
			Duration readTimeoutRemote, Duration connectTimeoutRemote, boolean logRequestsAndResponsesRemote,
			BpeProxyConfig proxyConfig, BuildInfoProvider buildInfoProvider)
	{
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.referenceCleaner = Objects.requireNonNull(referenceCleaner, "referenceCleaner");

		this.baseUrlLocal = Objects.requireNonNull(baseUrlLocal, "baseUrlLocal");
		this.readTimeoutLocal = Objects.requireNonNull(readTimeoutLocal, "readTimeoutLocal");
		this.connectTimeoutLocal = Objects.requireNonNull(connectTimeoutLocal, "connectTimeoutLocal");
		this.logRequestsAndResponsesLocal = logRequestsAndResponsesLocal;

		this.trustStore = Objects.requireNonNull(trustStore, "trustStore");
		this.keyStore = Objects.requireNonNull(keyStore, "keyStore");
		this.keyStorePassword = Objects.requireNonNull(keyStorePassword, "keyStorePassword");

		this.readTimeoutRemote = Objects.requireNonNull(readTimeoutRemote, "readTimeoutRemote");
		this.connectTimeoutRemote = Objects.requireNonNull(connectTimeoutRemote, "connectTimeoutRemote");
		this.logRequestsAndResponsesRemote = logRequestsAndResponsesRemote;

		this.proxyConfig = Objects.requireNonNull(proxyConfig, "proxyConfig");
		this.buildInfoProvider = Objects.requireNonNull(buildInfoProvider, "buildInfoProvider");
	}

	public String getLocalBaseUrl()
	{
		return baseUrlLocal;
	}

	private FhirWebserviceClient getClient(String webserviceUrl)
	{
		synchronized (webserviceClientsByUrl)
		{
			if (webserviceClientsByUrl.containsKey(webserviceUrl))
				return webserviceClientsByUrl.get(webserviceUrl);
			else
			{
				String proxyUrl = proxyConfig.isEnabled(webserviceUrl) ? proxyConfig.getUrl() : null;
				String proxyUsername = proxyConfig.isEnabled(webserviceUrl) ? proxyConfig.getUsername() : null;
				char[] proxyPassword = proxyConfig.isEnabled(webserviceUrl) ? proxyConfig.getPassword() : null;

				FhirWebserviceClient client;
				if (baseUrlLocal.equals(webserviceUrl))
					client = new FhirWebserviceClientJersey(webserviceUrl, trustStore, keyStore, keyStorePassword, null,
							proxyUrl, proxyUsername, proxyPassword, connectTimeoutLocal, readTimeoutLocal,
							logRequestsAndResponsesLocal, USER_AGENT_VALUE + buildInfoProvider.getProjectVersion(),
							fhirContext, referenceCleaner);
				else
					client = new FhirWebserviceClientJersey(webserviceUrl, trustStore, keyStore, keyStorePassword, null,
							proxyUrl, proxyUsername, proxyPassword, connectTimeoutRemote, readTimeoutRemote,
							logRequestsAndResponsesRemote, USER_AGENT_VALUE + buildInfoProvider.getProjectVersion(),
							fhirContext, referenceCleaner);

				webserviceClientsByUrl.put(webserviceUrl, client);
				return client;
			}
		}
	}

	@Override
	public FhirWebserviceClient getLocalWebserviceClient()
	{
		return getWebserviceClient(baseUrlLocal);
	}

	@Override
	public FhirWebserviceClient getWebserviceClient(String webserviceUrl)
	{
		Objects.requireNonNull(webserviceUrl, "webserviceUrl");

		FhirWebserviceClient cachedClient = webserviceClientsByUrl.get(webserviceUrl);
		if (cachedClient != null)
			return cachedClient;
		else
		{
			FhirWebserviceClient newClient = getClient(webserviceUrl);
			webserviceClientsByUrl.put(webserviceUrl, newClient);
			return newClient;
		}
	}
}
