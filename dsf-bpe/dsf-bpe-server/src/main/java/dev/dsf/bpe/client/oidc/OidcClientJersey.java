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

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.AlgorithmMismatchException;
import com.auth0.jwt.exceptions.IncorrectClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Verification;

import dev.dsf.bpe.api.client.oidc.Configuration;
import dev.dsf.bpe.api.client.oidc.OidcClientException;
import dev.dsf.bpe.client.oidc.JwksImpl.JwksKeyImpl;
import dev.dsf.common.oidc.BaseOidcClientJersey;
import dev.dsf.common.oidc.Jwks;
import dev.dsf.common.oidc.Jwks.JwksKey;
import dev.dsf.common.oidc.OidcConfiguration;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class OidcClientJersey extends BaseOidcClientJersey
{
	private static final Logger logger = LoggerFactory.getLogger(OidcClientJersey.class);

	private static final java.util.logging.Logger requestDebugLogger;
	static
	{
		requestDebugLogger = java.util.logging.Logger.getLogger(OidcClientJersey.class.getName());
		requestDebugLogger.setLevel(Level.INFO);
	}

	private final String clientId;
	private final char[] clientSecret;
	private final Duration notBeforeIssuedAtExpiresAtLeewaySeconds;
	private final List<String> requiredAudiences = new ArrayList<>();
	private final boolean verifyAuthorizedParty;

	/**
	 * @param baseUrl
	 *            not <code>null</code>
	 * @param discoveryPath
	 *            not <code>null</code>
	 * @param clientId
	 *            not <code>null</code>
	 * @param clientSecret
	 *            not <code>null</code>
	 * @param trustStore
	 *            may be <code>null</code>
	 * @param keyStore
	 *            may be <code>null</code>
	 * @param keyStorePassword
	 *            may be <code>null</code>
	 * @param proxySchemeHostPort
	 *            may be <code>null</code>
	 * @param proxyUserName
	 *            may be <code>null</code>
	 * @param proxyPassword
	 *            may be <code>null</code>
	 * @param userAgentValue
	 *            may be <code>null</code>
	 * @param connectTimeout
	 *            not <code>null</code>
	 * @param readTimeout
	 *            not <code>null</code>
	 * @param logRequestsAndResponses
	 * @param notBeforeIssuedAtExpiresAtLeewaySeconds
	 *            not <code>null</code>
	 * @param requiredAudiences
	 *            may be <code>null</code>
	 * @param verifyAuthorizedParty
	 */
	public OidcClientJersey(String baseUrl, String discoveryPath, String clientId, char[] clientSecret,
			KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, String proxySchemeHostPort,
			String proxyUserName, char[] proxyPassword, String userAgentValue, Duration connectTimeout,
			Duration readTimeout, boolean logRequestsAndResponses, Duration notBeforeIssuedAtExpiresAtLeewaySeconds,
			List<String> requiredAudiences, boolean verifyAuthorizedParty)
	{
		super(baseUrl, discoveryPath, trustStore, keyStore, keyStorePassword, proxySchemeHostPort, proxyUserName,
				proxyPassword, userAgentValue, connectTimeout, readTimeout, logRequestsAndResponses);

		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.notBeforeIssuedAtExpiresAtLeewaySeconds = Objects.requireNonNull(notBeforeIssuedAtExpiresAtLeewaySeconds,
				"notBeforeIssuedAtExpiresAtLeewaySeconds");
		if (requiredAudiences != null)
			this.requiredAudiences.addAll(requiredAudiences);
		this.verifyAuthorizedParty = verifyAuthorizedParty;
	}

	private void logUnexpectedResponseAndClose(Response response)
	{
		String message = response.readEntity(String.class);
		logger.debug("Unexpected response, status: {} {}, message: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase(), message);
	}

	public DecodedJWT getAccessTokenDecoded() throws OidcClientException
	{
		return getAccessTokenDecoded(getConfiguration(), getJwks());
	}

	public DecodedJWT getAccessTokenDecoded(OidcConfiguration configuration, Jwks jwks) throws OidcClientException
	{
		Objects.requireNonNull(configuration, "configuration");
		Objects.requireNonNull(jwks, "jwks");

		if (!configuration.grantTypesSupported().contains("client_credentials"))
			throw new OidcClientException(
					"OIDC provider does not support Client Credentials Grant, supported grant types: "
							+ configuration.grantTypesSupported());

		Response response = client.target(configuration.tokenEndpoint()).request(MediaType.APPLICATION_JSON_TYPE)
				.header(HttpHeaders.AUTHORIZATION,
						"Basic " + Base64.getEncoder()
								.encodeToString(new StringBuilder().append(clientId).append(':').append(clientSecret)
										.toString().getBytes(StandardCharsets.US_ASCII)))
				.post(Entity.form(new Form().param("grant_type", "client_credentials")));

		if (response.getStatus() == Status.OK.getStatusCode())
		{
			TokenResult result = response.readEntity(TokenResult.class);
			return verifyAndDecodeAccessToken(result.getAccessToken(), jwks);
		}
		else
		{
			logUnexpectedResponseAndClose(response);
			throw new OidcClientException("Unexpected response status code " + response.getStatusInfo().getStatusCode()
					+ " " + response.getStatusInfo().getReasonPhrase());
		}
	}

	/**
	 * Does not verify if the access token is expired. Supported algorithms: RS256, RS384, RS512, ES256, ES384 and
	 * ES512.
	 *
	 * @param accessToken
	 *            not <code>null</code>
	 * @param jwks
	 *            not <code>null</code>
	 * @return decoded access token
	 * @throws OidcClientException
	 *             if verification fails, the public key to verify is unknown or a unsupported signature algorithm was
	 *             used.
	 *
	 * @see DecodedJWT#getExpiresAt()
	 * @see DecodedJWT#getExpiresAtAsInstant()
	 */
	private DecodedJWT verifyAndDecodeAccessToken(String accessToken, Jwks jwks) throws OidcClientException
	{
		try
		{
			DecodedJWT decoded = JWT.decode(accessToken);
			String keyId = decoded.getKeyId();

			if (keyId == null)
				throw new OidcClientException("Access token has no kid property");

			Optional<JwksKey> key = jwks.getKey(keyId);
			if (key.isEmpty())
				throw new OidcClientException("Access token key with kid '" + keyId + "' not in JWKS");

			Optional<Algorithm> algorithm = key.map(JwksKey::toAlgorithm);
			if (key.isEmpty())
				throw new OidcClientException("Access token key with kid '" + keyId
						+ "' has unsupported type (kty) / algorithm (alg) in JWKS '" + key.get().kty() + "' / '"
						+ key.get().alg() + "'");

			try
			{
				Verification v = JWT.require(algorithm.get())
						.acceptLeeway(notBeforeIssuedAtExpiresAtLeewaySeconds.getSeconds());

				if (requiredAudiences.size() == 1)
					v.withAudience(requiredAudiences.get(0));
				else if (requiredAudiences.size() > 1)
					v.withAudience(requiredAudiences.toArray(String[]::new));

				if (verifyAuthorizedParty)
					v.withClaim("azp", clientId);

				return v.build().verify(decoded);
			}
			catch (AlgorithmMismatchException e)
			{
				throw new OidcClientException(
						"JWT verification failed: algorithm not '" + algorithm.get().getName() + "'", e);
			}
			catch (SignatureVerificationException e)
			{
				throw new OidcClientException("JWT verification failed: signature invalid", e);
			}
			catch (TokenExpiredException e)
			{
				throw new OidcClientException("JWT verification failed: claim missing", e);
			}
			catch (IncorrectClaimException e)
			{
				throw new OidcClientException("JWT verification failed: claim contained unexpected value", e);
			}
			catch (JWTVerificationException e)
			{
				throw new OidcClientException("JWT verification failed", e);
			}
		}
		catch (JWTDecodeException e)
		{
			throw new OidcClientException("Unable to parse access token", e);
		}
	}

	public OidcClientWithDecodedJwt asOidcClientWithDecodedJwt()
	{
		return new OidcClientWithDecodedJwt()
		{
			@Override
			public dev.dsf.bpe.api.client.oidc.Jwks getJwks(Configuration configuration) throws OidcClientException
			{
				return toJwksImpl(OidcClientJersey.this.getJwks(toOidcConfiguration(configuration)));
			}

			@Override
			public Configuration getConfiguration() throws OidcClientException
			{
				return toConfigurationImpl(OidcClientJersey.this.getConfiguration());
			}

			@Override
			public DecodedJWT getAccessTokenDecoded(Configuration configuration, dev.dsf.bpe.api.client.oidc.Jwks jwks)
					throws OidcClientException
			{
				return OidcClientJersey.this.getAccessTokenDecoded(toOidcConfiguration(configuration), toJwks(jwks));
			}

			@Override
			public DecodedJWT getAccessTokenDecoded() throws OidcClientException
			{
				return OidcClientJersey.this.getAccessTokenDecoded();
			}

			private OidcConfiguration toOidcConfiguration(Configuration configuration)
			{
				return new OidcConfiguration(configuration.getIssuer(), configuration.getTokenEndpoint(),
						configuration.getJwksUri(), configuration.getGrantTypesSupported());
			}

			private dev.dsf.bpe.api.client.oidc.Jwks toJwksImpl(Jwks jwks)
			{
				return new JwksImpl(jwks.getKeys().stream().map(
						k -> new JwksKeyImpl(k.kid(), k.kty(), k.alg(), k.crv(), k.use(), k.n(), k.e(), k.x(), k.y()))
						.toList());
			}

			private Configuration toConfigurationImpl(OidcConfiguration configuration)
			{
				return new ConfigurationImpl(configuration.issuer(), configuration.tokenEndpoint(),
						configuration.jwksUri(), configuration.grantTypesSupported());
			}

			private Jwks toJwks(dev.dsf.bpe.api.client.oidc.Jwks jwks)
			{
				return new Jwks(jwks.getKeys().stream().map(k -> new JwksKey(k.getKid(), k.getKty(), k.getAlg(),
						k.getCrv(), k.getUse(), k.getN(), k.getE(), k.getX(), k.getY())).toList());
			}
		};
	}
}