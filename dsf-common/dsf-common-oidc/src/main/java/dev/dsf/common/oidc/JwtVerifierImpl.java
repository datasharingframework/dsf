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

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.auth0.jwt.interfaces.Verification;

public class JwtVerifierImpl implements JwtVerifier
{
	private final String clientId;
	private final String bearerTokenAudience;

	private final BaseJwtVerifier backchannelLogoutTokenVerifier;
	private final BaseJwtVerifier bearerTokenVerifier;

	/**
	 * @param issuer
	 *            not <code>null</code>
	 * @param clientId
	 *            not <code>null</code>
	 * @param bearerTokenAudience
	 *            if <code>null</code>, uses value from <b>clientId</b>, will not check audience if value is blank
	 * @param bearerTokenNotBeforeIssuedAtExpiresAtLeeway
	 *            not <code>null</code>
	 * @param backchannelLogoutNotBeforeIssuedAtExpiresAtLeeway
	 *            not <code>null</code>
	 * @param oidcClient
	 *            not <code>null</code>
	 */
	public JwtVerifierImpl(String issuer, String clientId, String bearerTokenAudience,
			Duration bearerTokenNotBeforeIssuedAtExpiresAtLeeway,
			Duration backchannelLogoutNotBeforeIssuedAtExpiresAtLeeway, BaseOidcClient oidcClient)
	{
		this.clientId = Objects.requireNonNull(clientId, "clientId");
		this.bearerTokenAudience = bearerTokenAudience == null ? clientId : bearerTokenAudience;

		this.backchannelLogoutTokenVerifier = new BaseJwtVerifierImpl("Logout", issuer, oidcClient::getJwks,
				backchannelLogoutNotBeforeIssuedAtExpiresAtLeeway);
		this.bearerTokenVerifier = new BaseJwtVerifierImpl("Bearer", issuer, oidcClient::getJwks,
				bearerTokenNotBeforeIssuedAtExpiresAtLeeway);
	}

	@Override
	public DecodedJWT verifyBackchannelLogoutToken(String token) throws JWTVerificationException, OidcClientException
	{
		JWTVerifier verifier = backchannelLogoutTokenVerifier.createVerification(token).withAudience(clientId)
				.withClaim("events",
						(claim, _) -> claim.asMap().containsKey("http://schemas.openid.net/event/backchannel-logout"))
				.build();

		return verifier.verify(token);
	}

	@Override
	public DecodedJWT verifyBearerToken(String token) throws JWTVerificationException, OidcClientException
	{
		Verification verification = bearerTokenVerifier.createVerification(token);

		if (!bearerTokenAudience.isBlank())
			verification.withAnyOfAudience(bearerTokenAudience);

		JWTVerifier verifier = verification.build();

		return verifier.verify(token);
	}
}
