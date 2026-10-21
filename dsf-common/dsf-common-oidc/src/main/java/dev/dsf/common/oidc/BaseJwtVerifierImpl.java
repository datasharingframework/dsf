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

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

import dev.dsf.common.oidc.Jwks.JwksKey;

public class BaseJwtVerifierImpl implements BaseJwtVerifier
{
	private final String tokenType;
	private final String issuer;
	private final Supplier<Jwks> jwksSupplier;
	private final Duration notBeforeIssuedAtExpiresAtLeeway;

	/**
	 * @param tokenType
	 *            not <code>null</code>
	 * @param issuer
	 *            not <code>null</code>
	 * @param jwksSupplier
	 *            not <code>null</code>
	 * @param notBeforeIssuedAtExpiresAtLeeway
	 *            not <code>null</code>
	 */
	public BaseJwtVerifierImpl(String tokenType, String issuer, Supplier<Jwks> jwksSupplier,
			Duration notBeforeIssuedAtExpiresAtLeeway)
	{
		this.tokenType = Objects.requireNonNull(tokenType, "tokenType");
		this.issuer = Objects.requireNonNull(issuer, "issuer");
		this.jwksSupplier = Objects.requireNonNull(jwksSupplier, "jwksSupplier");
		this.notBeforeIssuedAtExpiresAtLeeway = Objects.requireNonNull(notBeforeIssuedAtExpiresAtLeeway,
				"notBeforeIssuedAtExpiresAtLeeway");
	}

	@Override
	public Verification createVerification(String token) throws OidcClientException
	{
		DecodedJWT decoded = JWT.decode(token);
		String keyId = decoded.getKeyId();
		String alg = decoded.getAlgorithm();

		Optional<JwksKey> key = jwksSupplier.get().getKey(keyId);
		if (key.isEmpty())
			throw new OidcClientException(tokenType + " token key not in JWKS");
		else if (!key.get().isSignatureVerifyKey())
			throw new OidcClientException(tokenType + " token key with use 'sig' or key_ops 'verify' not in JWKS");

		Optional<Algorithm> algorithm = key.flatMap(k -> k.toAlgorithm(alg));
		if (algorithm.isEmpty())
			throw new OidcClientException(
					tokenType + " token key has unsupported type (kty) / algorithm (alg) / key-size");

		return JWT.require(algorithm.get()).withIssuer(issuer)
				.acceptLeeway(notBeforeIssuedAtExpiresAtLeeway.toSeconds());
	}
}
