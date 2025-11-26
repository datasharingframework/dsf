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
package dev.dsf.bpe.api.client.oidc;

public interface OidcClient
{
	/**
	 * @return OIDC {@link Configuration} resource
	 * @throws OidcClientException
	 *             if response status not 200 OK, response issuer not matching base-url or response supported grant
	 *             types does not include <code>"client_credentials"</code>
	 */
	Configuration getConfiguration() throws OidcClientException;

	/**
	 * @return {@link Jwks} resource
	 * @throws OidcClientException
	 *             if response status not 200 OK
	 */
	default Jwks getJwks() throws OidcClientException
	{
		return getJwks(getConfiguration());
	}

	/**
	 * <i>Implementation may ignore the configuration parameter and use value from {@link #getConfiguration()}
	 * instead.</i>
	 *
	 * @param configuration
	 *            may be <code>null</code>, uses value from {@link #getConfiguration()} if <code>null</code>
	 * @return {@link Jwks} resource
	 * @throws OidcClientException
	 *             if response status not 200 OK
	 */
	Jwks getJwks(Configuration configuration) throws OidcClientException;

	/**
	 * @return access token
	 */
	char[] getAccessToken() throws OidcClientException;

	/**
	 * @param configuration
	 *            not <code>null</code>
	 * @param jwks
	 *            not <code>null</code>
	 * @return access token
	 * @throws OidcClientException
	 *             if response status not 200 OK, OIDC provider does not support client credentials grant (Keycloak:
	 *             service accounts roles) or returned access token could not be verified
	 */
	char[] getAccessToken(Configuration configuration, Jwks jwks) throws OidcClientException;
}