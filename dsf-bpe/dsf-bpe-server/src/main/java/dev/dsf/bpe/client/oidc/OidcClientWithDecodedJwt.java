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

import com.auth0.jwt.interfaces.DecodedJWT;

import dev.dsf.bpe.api.client.oidc.Configuration;
import dev.dsf.bpe.api.client.oidc.Jwks;
import dev.dsf.bpe.api.client.oidc.OidcClient;
import dev.dsf.bpe.api.client.oidc.OidcClientException;

public interface OidcClientWithDecodedJwt extends OidcClient
{
	DecodedJWT getAccessTokenDecoded() throws OidcClientException;

	DecodedJWT getAccessTokenDecoded(Configuration configuration, Jwks jwks) throws OidcClientException;

	@Override
	default char[] getAccessToken() throws OidcClientException
	{
		return getAccessTokenDecoded().getToken().toCharArray();
	}

	@Override
	default char[] getAccessToken(Configuration configuration, Jwks jwks) throws OidcClientException
	{
		return getAccessTokenDecoded(configuration, jwks).getToken().toCharArray();
	}
}
