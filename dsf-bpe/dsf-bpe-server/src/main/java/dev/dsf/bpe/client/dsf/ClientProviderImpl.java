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
package dev.dsf.bpe.client.dsf;

import java.net.URI;
import java.security.KeyStore;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.config.ProxyConfig;
import dev.dsf.fhir.client.WebsocketClient;
import dev.dsf.fhir.client.WebsocketClientTyrus;

public class ClientProviderImpl implements ClientProvider
{
	private static final Logger logger = LoggerFactory.getLogger(ClientProviderImpl.class);

	private final Map<String, WebsocketClient> websocketClientsBySubscriptionId = new HashMap<>();

	private final String baseUrl;

	private final String websocketUrl;
	private final KeyStore websocketTrustStore;
	private final KeyStore websocketKeyStore;
	private final char[] websocketKeyStorePassword;

	private final ProxyConfig proxyConfig;
	private final String userAgent;

	private final WebserviceClient webserviceClient;

	private final KeyStore webserviceTrustStore;
	private final KeyStore webserviceKeyStore;
	private final char[] webserviceKeyStorePassword;

	/**
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param baseUrl
	 *            not <code>null</code>
	 * @param readTimeout
	 *            not <code>null</code>
	 * @param connectTimeout
	 *            not <code>null</code>
	 * @param logRequestsAndResponses
	 * @param webserviceTrustStore
	 *            not <code>null</code>
	 * @param webserviceKeyStore
	 *            not <code>null</code>
	 * @param webserviceKeyStorePassword
	 *            not <code>null</code>
	 * @param websocketUrl
	 *            not <code>null</code>
	 * @param websocketTrustStore
	 *            not <code>null</code>
	 * @param websocketKeyStore
	 *            not <code>null</code>
	 * @param websocketKeyStorePassword
	 *            not <code>null</code>
	 * @param proxyConfig
	 *            not <code>null</code>
	 * @param userAgent
	 *            not <code>null</code>
	 */
	public ClientProviderImpl(FhirContext fhirContext, String baseUrl, Duration readTimeout, Duration connectTimeout,
			boolean logRequestsAndResponses, KeyStore webserviceTrustStore, KeyStore webserviceKeyStore,
			char[] webserviceKeyStorePassword, String websocketUrl, KeyStore websocketTrustStore,
			KeyStore websocketKeyStore, char[] websocketKeyStorePassword, ProxyConfig proxyConfig, String userAgent)
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(baseUrl, "baseUrl");
		Objects.requireNonNull(readTimeout, "readTimeout");
		Objects.requireNonNull(connectTimeout, "connectTimeout");
		Objects.requireNonNull(webserviceTrustStore, "webserviceTrustStore");
		Objects.requireNonNull(webserviceKeyStore, "webserviceKeyStore");
		Objects.requireNonNull(webserviceKeyStorePassword, "webserviceKeyStorePassword");

		this.baseUrl = baseUrl;

		this.websocketUrl = Objects.requireNonNull(websocketUrl, "websocketUrl");
		this.websocketTrustStore = Objects.requireNonNull(websocketTrustStore, "websocketTrustStore");
		this.websocketKeyStore = Objects.requireNonNull(websocketKeyStore, "websocketKeyStore");
		this.websocketKeyStorePassword = Objects.requireNonNull(websocketKeyStorePassword, "websocketKeyStorePassword");
		this.proxyConfig = Objects.requireNonNull(proxyConfig, "proxyConfig");
		this.userAgent = Objects.requireNonNull(userAgent, "userAgent");

		String proxyUrl = proxyConfig.isEnabled(baseUrl) ? proxyConfig.getUrl() : null;
		String proxyUsername = proxyConfig.isEnabled(baseUrl) ? proxyConfig.getUsername() : null;
		char[] proxyPassword = proxyConfig.isEnabled(baseUrl) ? proxyConfig.getPassword() : null;

		webserviceClient = new WebserviceClientJersey(baseUrl, webserviceTrustStore, webserviceKeyStore,
				webserviceKeyStorePassword, proxyUrl, proxyUsername, proxyPassword, connectTimeout, readTimeout,
				logRequestsAndResponses, userAgent, fhirContext);

		this.webserviceTrustStore = webserviceTrustStore;
		this.webserviceKeyStore = webserviceKeyStore;
		this.webserviceKeyStorePassword = webserviceKeyStorePassword;
	}

	public String getBaseUrl()
	{
		return baseUrl;
	}

	@Override
	public WebserviceClient getWebserviceClient()
	{
		return webserviceClient;
	}

	@Override
	public WebsocketClient getWebsocketClient(Runnable reconnector, String subscriptionId)
	{
		if (!websocketClientsBySubscriptionId.containsKey(subscriptionId))
		{
			WebsocketClientTyrus client = createWebsocketClient(reconnector, subscriptionId);
			websocketClientsBySubscriptionId.put(subscriptionId, client);
			return client;
		}

		return websocketClientsBySubscriptionId.get(subscriptionId);
	}

	protected WebsocketClientTyrus createWebsocketClient(Runnable reconnector, String subscriptionId)
	{
		return new WebsocketClientTyrus(reconnector, URI.create(websocketUrl), websocketTrustStore, websocketKeyStore,
				websocketKeyStorePassword, proxyConfig.isEnabled(websocketUrl) ? proxyConfig.getUrl() : null,
				proxyConfig.isEnabled(websocketUrl) ? proxyConfig.getUsername() : null,
				proxyConfig.isEnabled(websocketUrl) ? proxyConfig.getPassword() : null, userAgent, subscriptionId);
	}

	@Override
	public void disconnectAll()
	{
		for (WebsocketClient c : websocketClientsBySubscriptionId.values())
		{
			try
			{
				c.disconnect();
			}
			catch (Exception e)
			{
				logger.debug("Error while disconnecting websocket client", e);
				logger.warn("Error while disconnecting websocket client: {} - {}", e.getClass().getName(),
						e.getMessage());
			}
		}
	}

	@Override
	public KeyStore getWebserviceTrustStore()
	{
		return webserviceTrustStore;
	}

	@Override
	public KeyStore getWebserviceKeyStore()
	{
		return webserviceKeyStore;
	}

	@Override
	public char[] getWebserviceKeyStorePassword()
	{
		return webserviceKeyStorePassword;
	}
}
