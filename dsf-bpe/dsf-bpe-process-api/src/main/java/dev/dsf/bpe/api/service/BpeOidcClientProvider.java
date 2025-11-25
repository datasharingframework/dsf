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
package dev.dsf.bpe.api.service;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

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
		return getOidcClient(baseUrl, clientId, clientSecret, clientId, null, null, null, null, null, null);
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
	 * @param requiredAudiences
	 *            may be <code>null</code> or empty
	 * @param verifyAuthorizedParty
	 *            may be <code>null</code>, will use configured default value
	 * @return never <code>null</code>
	 */
	OidcClient getOidcClient(String baseUrl, String clientId, char[] clientSecret, String discoveryPath,
			Duration connectTimeout, Duration readTimeout, KeyStore trustStore, Boolean enableDebugLogging,
			List<String> requiredAudiences, Boolean verifyAuthorizedParty);

	/**
	 * @param config
	 *            not <code>null</code>
	 * @return never <code>null</code>
	 */
	OidcClient getOidcClient(OidcAuthentication config);
}