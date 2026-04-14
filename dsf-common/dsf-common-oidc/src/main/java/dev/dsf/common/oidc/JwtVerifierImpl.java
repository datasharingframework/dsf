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
package dev.dsf.common.oidc;

import java.util.Objects;
import java.util.Optional;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.Verification;

import dev.dsf.common.oidc.Jwks.JwksKey;

public class JwtVerifierImpl implements JwtVerifier
{
	private final String issuer;
	private final String clientId;
	private final String bearerTokenAudience;

	private final BaseOidcClient oidcClient;

	/**
	 * @param issuer
	 *            not <code>null</code>
	 * @param clientId
	 *            not <code>null</code>
	 * @param bearerTokenAudience
	 *            if <code>null</code>, uses value from <b>clientId</b>, will not check audience if value is blank
	 * @param oidcClient
	 *            not <code>null</code>
	 */
	public JwtVerifierImpl(String issuer, String clientId, String bearerTokenAudience, BaseOidcClient oidcClient)
	{
		this.issuer = Objects.requireNonNull(issuer, "issuer");
		this.clientId = Objects.requireNonNull(clientId, "clientId");
		this.bearerTokenAudience = bearerTokenAudience == null ? clientId : bearerTokenAudience;
		this.oidcClient = Objects.requireNonNull(oidcClient, "oidcClient");
	}

	@Override
	public DecodedJWT verifyBackchannelLogout(String token) throws JWTVerificationException, OidcClientException
	{
		final String keyId = JWT.decode(token).getKeyId();

		Optional<JwksKey> key = oidcClient.getJwks().getKey(keyId);
		if (key.isEmpty() || !key.get().use().equals("sig"))
			throw new OidcClientException("Logout token key with kid '" + keyId + "'  and use 'sig' not in JWKS");

		Optional<Algorithm> algorithm = key.flatMap(JwksKey::toAlgorithm);
		if (key.isEmpty())
		{
			throw new OidcClientException("Logout token key with kid '" + keyId
					+ "' has unsupported type (kty) / algorithm (alg) / key-size in JWKS");
		}

		JWTVerifier verifier = createVerification(algorithm.get()).withAudience(clientId)
				.withClaim("events",
						(claim, _) -> claim.asMap().containsKey("http://schemas.openid.net/event/backchannel-logout"))
				.build();

		return verifier.verify(token);
	}

	private Verification createVerification(Algorithm algorithm)
	{
		return JWT.require(algorithm).withIssuer(issuer).acceptLeeway(1);
	}

	@Override
	public DecodedJWT verifyBearerToken(String token) throws JWTVerificationException, OidcClientException
	{
		final String keyId = JWT.decode(token).getKeyId();

		Optional<JwksKey> key = oidcClient.getJwks().getKey(keyId);
		if (key.isEmpty() || !key.get().use().equals("sig"))
			throw new OidcClientException("Bearer token key with kid '" + keyId + "'  and use 'sig' not in JWKS");

		Optional<Algorithm> algorithm = key.flatMap(JwksKey::toAlgorithm);
		if (key.isEmpty())
		{
			throw new OidcClientException("Bearer token key with kid '" + keyId
					+ "' has unsupported type (kty) / algorithm (alg) / key-size in JWKS");
		}

		Verification verification = createVerification(algorithm.get());

		if (!bearerTokenAudience.isBlank())
			verification.withAnyOfAudience(bearerTokenAudience);

		JWTVerifier verifier = verification.build();

		return verifier.verify(token);
	}
}
