package dev.dsf.bpe.client.oidc;

import java.security.KeyStore;
import java.time.Duration;
import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.api.client.oidc.OidcClient;
import dev.dsf.bpe.api.config.FhirClientConfig.OidcAuthentication;
import dev.dsf.bpe.api.service.BpeOidcClientProvider;
import dev.dsf.common.config.ProxyConfig;

public class OidcClientProviderImpl implements BpeOidcClientProvider, InitializingBean
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

	public OidcClientProviderImpl(ProxyConfig proxyConfig, String defaultDiscoveryPath, Duration defaultConnectTimeout,
			Duration defaultReadTimeout, KeyStore defaultTrustedStore, boolean defaultEnableDebugLogging,
			String userAgent, boolean cacheEnabled, Duration cacheTimeoutConfigurationResource,
			Duration cacheTimeoutJwksResource, Duration cacheTimeoutAccessTokenBeforeExpiration,
			Duration notBeforeIssuedAtExpiresAtLeeway)
	{
		this.proxyConfig = proxyConfig;
		this.defaultDiscoveryPath = defaultDiscoveryPath;
		this.defaultConnectTimeout = defaultConnectTimeout;
		this.defaultReadTimeout = defaultReadTimeout;
		this.defaultTrustedStore = defaultTrustedStore;
		this.defaultEnableDebugLogging = defaultEnableDebugLogging;
		this.userAgent = userAgent;
		this.cacheEnabled = cacheEnabled;
		this.cacheTimeoutConfigurationResource = cacheTimeoutConfigurationResource;
		this.cacheTimeoutJwksResource = cacheTimeoutJwksResource;
		this.cacheTimeoutAccessTokenBeforeExpiration = cacheTimeoutAccessTokenBeforeExpiration;
		this.notBeforeIssuedAtExpiresAtLeeway = notBeforeIssuedAtExpiresAtLeeway;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(defaultConnectTimeout, "defaultConnectTimeout");
		Objects.requireNonNull(defaultReadTimeout, "defaultReadTimeout");
		Objects.requireNonNull(defaultTrustedStore, "defaultTrustedStore");
		Objects.requireNonNull(userAgent, "userAgent");

		Objects.requireNonNull(cacheTimeoutConfigurationResource, "cacheTimeoutConfigurationResource");
		if (cacheTimeoutConfigurationResource.isNegative())
			throw new IllegalArgumentException("cacheTimeoutConfigurationResource negative");

		Objects.requireNonNull(cacheTimeoutJwksResource, "cacheTimeoutJwksResource");
		if (cacheTimeoutJwksResource.isNegative())
			throw new IllegalArgumentException("cacheTimeoutJwksResource negative");

		Objects.requireNonNull(cacheTimeoutAccessTokenBeforeExpiration, "cacheTimeoutAccessTokenBeforeExpiration");
		if (cacheTimeoutAccessTokenBeforeExpiration.isNegative())
			throw new IllegalArgumentException("cacheTimeoutAccessTokenBeforeExpiration negative");

		Objects.requireNonNull(notBeforeIssuedAtExpiresAtLeeway, "notBeforeIssuedAtExpiresAtLeeway");
		if (notBeforeIssuedAtExpiresAtLeeway.isNegative())
			throw new IllegalArgumentException("notBeforeIssuedAtExpiresAtLeeway negative");
	}

	@Override
	public OidcClient getOidcClient(String baseUrl, String clientId, char[] clientSecret, String discoveryPath,
			Duration connectTimeout, Duration readTimeout, KeyStore trustStore, Boolean enableDebugLogging)
	{
		Objects.requireNonNull(baseUrl, "baseUrl");
		Objects.requireNonNull(clientId, "clientId");
		Objects.requireNonNull(clientSecret, "clientSecret");

		String proxyHost = null, proxyUsername = null;
		char[] proxyPassowrd = null;
		if (proxyConfig.isEnabled(baseUrl))
		{
			proxyHost = proxyConfig.getUrl();
			proxyUsername = proxyConfig.getUsername();
			proxyPassowrd = proxyConfig.getPassword();
		}

		OidcClientJersey client = new OidcClientJersey(baseUrl,
				discoveryPath != null ? discoveryPath : defaultDiscoveryPath, clientId, clientSecret,
				trustStore != null ? trustStore : defaultTrustedStore, null, null, proxyHost, proxyUsername,
				proxyPassowrd, userAgent, readTimeout != null ? readTimeout : defaultReadTimeout,
				connectTimeout != null ? connectTimeout : defaultConnectTimeout,
				enableDebugLogging != null ? enableDebugLogging : defaultEnableDebugLogging,
				notBeforeIssuedAtExpiresAtLeeway);

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
				config.connectTimeout(), config.readTimeout(), config.trustStore(), config.enableDebugLogging());
	}
}
