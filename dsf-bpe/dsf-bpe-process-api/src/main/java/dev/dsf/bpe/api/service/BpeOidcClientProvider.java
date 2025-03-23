package dev.dsf.bpe.api.service;

import java.security.KeyStore;
import java.time.Duration;

import dev.dsf.bpe.api.client.oidc.OidcClient;
import dev.dsf.bpe.api.config.FhirClientConfig.OidcAuthentication;

public interface BpeOidcClientProvider
{
	/**
	 * @param baseUrl
	 *            not <code>null</code>
	 * @param clientId
	 *            not <code>null</code>
	 * @param clientSecret
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 */
	default OidcClient getOidcClient(String baseUrl, String clientId, char[] clientSecret)
	{
		return getOidcClient(baseUrl, clientId, clientSecret, clientId, null, null, null, null);
	}

	/**
	 * @param baseUrl
	 *            not <code>null</code>
	 * @param clientId
	 *            not <code>null</code>
	 * @param clientSecret
	 *            not <code>null</code>
	 * @param discoveryPath
	 *            may be <code>null</code>, will use configured default value
	 * @param connectTimeout
	 *            may be <code>null</code>, will use configured default value
	 * @param readTimeout
	 *            may be <code>null</code>, will use configured default value
	 * @param trustStore
	 *            may be <code>null</code>, will use configured default value
	 * @param enableDebugLogging
	 *            may be <code>null</code>, will use configured default value
	 * @return never <code>null</code>
	 */
	OidcClient getOidcClient(String baseUrl, String clientId, char[] clientSecret, String discoveryPath,
			Duration connectTimeout, Duration readTimeout, KeyStore trustStore, Boolean enableDebugLogging);

	/**
	 * @param config
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 */
	OidcClient getOidcClient(OidcAuthentication config);
}