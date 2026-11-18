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
import java.util.Objects;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.config.ProxyConfig;
import dev.dsf.fhir.client.Endpoint;
import dev.dsf.fhir.client.WebsocketClient;
import dev.dsf.fhir.client.WebsocketClientJetty;
import dev.dsf.fhir.client.WebsocketClientJetty.TimeConfig;

public class ClientProviderImpl implements ClientProvider
{
	private final String baseUrl;

	private final String websocketUrl;
	private final TimeConfig websocketTimeConfig;
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
			char[] webserviceKeyStorePassword, String websocketUrl, TimeConfig websocketTimeConfig,
			KeyStore websocketTrustStore, KeyStore websocketKeyStore, char[] websocketKeyStorePassword,
			ProxyConfig proxyConfig, String userAgent)
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(baseUrl, "baseUrl");
		Objects.requireNonNull(connectTimeout, "connectTimeout");
		Objects.requireNonNull(readTimeout, "readTimeout");
		Objects.requireNonNull(websocketTimeConfig, "websocketTimeConfig");
		Objects.requireNonNull(webserviceTrustStore, "webserviceTrustStore");
		Objects.requireNonNull(webserviceKeyStore, "webserviceKeyStore");
		Objects.requireNonNull(webserviceKeyStorePassword, "webserviceKeyStorePassword");

		this.baseUrl = baseUrl;

		this.websocketUrl = Objects.requireNonNull(websocketUrl, "websocketUrl");
		this.websocketTimeConfig = Objects.requireNonNull(websocketTimeConfig, "websocketTimeConfig");
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
	public WebsocketClient createWebsocketClient(Endpoint endpoint, String resourceType)
	{
		SSLContext sslContext;
		if (websocketTrustStore != null && websocketKeyStore == null && websocketKeyStorePassword == null)
			sslContext = SslConfigurator.newInstance().trustStore(websocketTrustStore).createSSLContext();
		else if (websocketTrustStore != null && websocketKeyStore != null && websocketKeyStorePassword != null)
			sslContext = SslConfigurator.newInstance().trustStore(websocketTrustStore).keyStore(websocketKeyStore)
					.keyStorePassword(websocketKeyStorePassword).createSSLContext();
		else
			sslContext = SslConfigurator.getDefaultContext();

		WebsocketClientJetty.ProxyConfig proxyConfig = null;
		if (this.proxyConfig.isEnabled(websocketUrl))
			proxyConfig = new WebsocketClientJetty.ProxyConfig(URI.create(this.proxyConfig.getUrl()),
					this.proxyConfig.getUsername(), this.proxyConfig.getPassword());

		return new WebsocketClientJetty(URI.create(websocketUrl), endpoint, userAgent, resourceType,
				websocketTimeConfig, sslContext, proxyConfig);
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
