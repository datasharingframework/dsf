package dev.dsf.common.oidc;

import java.util.Objects;

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

		JWTVerifier verifier = oidcClient.getJwks().getKey(keyId).map(JwksKey::toAlgorithm).map(algorithm ->
		{
			return createVerification(algorithm).withAudience(clientId).withClaim("events",
					(claim, _) -> claim.asMap().containsKey("http://schemas.openid.net/event/backchannel-logout"))
					.build();

		}).orElseThrow(() -> new OidcClientException(
				"Key with id " + keyId + " not found in JWKS resource from OIDC provider"));

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

		JWTVerifier verifier = oidcClient.getJwks().getKey(keyId).map(JwksKey::toAlgorithm).map(algorithm ->
		{
			Verification verification = createVerification(algorithm).acceptLeeway(1);

			if (!bearerTokenAudience.isBlank())
				verification.withAnyOfAudience(bearerTokenAudience);

			return verification.build();

		}).orElseThrow(() -> new OidcClientException(
				"Key with id " + keyId + " not found in JWKS resource from OIDC provider"));

		return verifier.verify(token);
	}
}
