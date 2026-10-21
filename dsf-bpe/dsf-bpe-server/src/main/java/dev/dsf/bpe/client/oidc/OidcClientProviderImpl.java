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
package dev.dsf.bpe.client.oidc;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import dev.dsf.bpe.api.client.oidc.OidcClient;
import dev.dsf.bpe.api.config.FhirClientConfig.OidcAuthentication;
import dev.dsf.bpe.api.service.BpeOidcClientProvider;
import dev.dsf.common.config.ProxyConfig;

public class OidcClientProviderImpl implements BpeOidcClientProvider
{
	private final ProxyConfig proxyConfig;
	private final String defaultDiscoveryPath;
	private final Duration defaultConnectTimeout;
	private final Duration defaultReadTimeout;
	private final KeyStore defaultTrustedStore;
	private final boolean defaultEnableDebugLogging;
	private final String userAgent;
	private final boolean cacheEnabled;
	private final Duration cacheTimeoutConfigurationResource;
	private final Duration cacheTimeoutJwksResource;
	private final Duration cacheTimeoutAccessTokenBeforeExpiration;
	private final Duration notBeforeIssuedAtExpiresAtLeeway;
	private final boolean defaultVerifyAuthorizedParty;

	/**
	 * @param proxyConfig
	 *            not <code>null</code>
	 * @param defaultDiscoveryPath
	 *            not <code>null</code>
	 * @param defaultConnectTimeout
	 *            not <code>null</code>, not negative, 0 represents infinity
	 * @param defaultReadTimeout
	 *            not <code>null</code>, not negative, 0 represents infinity
	 * @param defaultTrustedStore
	 *            not <code>null</code>
	 * @param defaultEnableDebugLogging
	 * @param userAgent
	 *            not <code>null</code>
	 * @param cacheEnabled
	 * @param cacheTimeoutConfigurationResource
	 *            not <code>null</code>, not negative
	 * @param cacheTimeoutJwksResource
	 *            not <code>null</code>, not negative
	 * @param cacheTimeoutAccessTokenBeforeExpiration
	 *            not <code>null</code>, not negative
	 * @param notBeforeIssuedAtExpiresAtLeeway
	 *            not <code>null</code>, not negative
	 * @param defaultVerifyAuthorizedParty
	 */
	public OidcClientProviderImpl(ProxyConfig proxyConfig, String defaultDiscoveryPath, Duration defaultConnectTimeout,
			Duration defaultReadTimeout, KeyStore defaultTrustedStore, boolean defaultEnableDebugLogging,
			String userAgent, boolean cacheEnabled, Duration cacheTimeoutConfigurationResource,
			Duration cacheTimeoutJwksResource, Duration cacheTimeoutAccessTokenBeforeExpiration,
			Duration notBeforeIssuedAtExpiresAtLeeway, boolean defaultVerifyAuthorizedParty)
	{
		this.proxyConfig = Objects.requireNonNull(proxyConfig, "proxyConfig");
		this.defaultDiscoveryPath = Objects.requireNonNull(defaultDiscoveryPath, "defaultDiscoveryPath");
		this.defaultConnectTimeout = Objects.requireNonNull(defaultConnectTimeout, "defaultConnectTimeout");
		if (defaultConnectTimeout.isNegative())
			throw new IllegalArgumentException("defaultConnectTimeout negative");
		this.defaultReadTimeout = Objects.requireNonNull(defaultReadTimeout, "defaultReadTimeout");
		if (defaultReadTimeout.isNegative())
			throw new IllegalArgumentException("defaultReadTimeout negative");
		this.defaultTrustedStore = Objects.requireNonNull(defaultTrustedStore, "defaultTrustedStore");
		this.defaultEnableDebugLogging = defaultEnableDebugLogging;
		this.userAgent = Objects.requireNonNull(userAgent, "userAgent");
		this.cacheEnabled = cacheEnabled;
		this.cacheTimeoutConfigurationResource = Objects.requireNonNull(cacheTimeoutConfigurationResource,
				"cacheTimeoutConfigurationResource");
		if (cacheTimeoutConfigurationResource.isNegative())
			throw new IllegalArgumentException("cacheTimeoutConfigurationResource negative");
		this.cacheTimeoutJwksResource = Objects.requireNonNull(cacheTimeoutJwksResource, "cacheTimeoutJwksResource");
		if (cacheTimeoutJwksResource.isNegative())
			throw new IllegalArgumentException("cacheTimeoutJwksResource negative");
		this.cacheTimeoutAccessTokenBeforeExpiration = Objects.requireNonNull(cacheTimeoutAccessTokenBeforeExpiration,
				"cacheTimeoutAccessTokenBeforeExpiration");
		if (cacheTimeoutAccessTokenBeforeExpiration.isNegative())
			throw new IllegalArgumentException("cacheTimeoutAccessTokenBeforeExpiration negative");
		this.notBeforeIssuedAtExpiresAtLeeway = Objects.requireNonNull(notBeforeIssuedAtExpiresAtLeeway,
				"notBeforeIssuedAtExpiresAtLeeway");
		if (notBeforeIssuedAtExpiresAtLeeway.isNegative())
			throw new IllegalArgumentException("notBeforeIssuedAtExpiresAtLeeway negative");
		this.defaultVerifyAuthorizedParty = defaultVerifyAuthorizedParty;
	}

	@Override
	public OidcClient getOidcClient(String baseUrl, String clientId, char[] clientSecret, String discoveryPath,
			Duration connectTimeout, Duration readTimeout, KeyStore trustStore, Boolean enableDebugLogging,
			List<String> requiredAudiences, Boolean verifyAuthorizedParty)
	{
		Objects.requireNonNull(baseUrl, "baseUrl");
		Objects.requireNonNull(clientId, "clientId");
		Objects.requireNonNull(clientSecret, "clientSecret");

		String proxyHost = null, proxyUsername = null;
		char[] proxyPassword = null;
		if (proxyConfig.isEnabled(baseUrl))
		{
			proxyHost = proxyConfig.getUrl();
			proxyUsername = proxyConfig.getUsername();
			proxyPassword = proxyConfig.getPassword();
		}

		OidcClientWithDecodedJwt client = new OidcClientJersey(baseUrl,
				discoveryPath != null ? discoveryPath : defaultDiscoveryPath, clientId, clientSecret,
				trustStore != null ? trustStore : defaultTrustedStore, null, null, proxyHost, proxyUsername,
				proxyPassword, userAgent, readTimeout != null ? readTimeout : defaultReadTimeout,
				connectTimeout != null ? connectTimeout : defaultConnectTimeout,
				enableDebugLogging != null ? enableDebugLogging : defaultEnableDebugLogging,
				notBeforeIssuedAtExpiresAtLeeway, requiredAudiences,
				verifyAuthorizedParty != null ? verifyAuthorizedParty : defaultVerifyAuthorizedParty)
				.asOidcClientWithDecodedJwt();

		return cacheEnabled
				? new OidcClientWithCache(cacheTimeoutConfigurationResource, cacheTimeoutJwksResource,
						cacheTimeoutAccessTokenBeforeExpiration, client)
				: client;
	}

	@Override
	public OidcClient getOidcClient(OidcAuthentication config)
	{
		Objects.requireNonNull(config, "config");

		return getOidcClient(config.baseUrl(), config.clientId(), config.clientSecret(), config.discoveryPath(),
				config.connectTimeout(), config.readTimeout(), config.trustStore(), config.debugLoggingEnabled(),
				config.requiredAudiences(), config.verifyAuthorizedParty());
	}
}
