package dev.dsf.bpe.client.oidc;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
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
import com.auth0.jwt.interfaces.ECDSAKeyProvider;
import com.auth0.jwt.interfaces.RSAKeyProvider;

import dev.dsf.bpe.api.client.oidc.Configuration;
import dev.dsf.bpe.api.client.oidc.Jwks;
import dev.dsf.bpe.api.client.oidc.Jwks.JwksKey;
import dev.dsf.bpe.api.client.oidc.OidcClientException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class OidcClientJersey implements OidcClientWithDecodedJwt
{
	private static final Logger logger = LoggerFactory.getLogger(OidcClientJersey.class);

	private static final java.util.logging.Logger requestDebugLogger;
	static
	{
		requestDebugLogger = java.util.logging.Logger.getLogger(OidcClientJersey.class.getName());
		requestDebugLogger.setLevel(Level.INFO);
	}

	private final String baseUrl;
	private final String discoveryPath;
	private final Duration notBeforeIssuedAtExpiresAtLeewaySeconds;

	private final String basicAuthorizationValue;

	private final Client client;

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
	 */
	public OidcClientJersey(String baseUrl, String discoveryPath, String clientId, char[] clientSecret,
			KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword, String proxySchemeHostPort,
			String proxyUserName, char[] proxyPassword, String userAgentValue, Duration connectTimeout,
			Duration readTimeout, boolean logRequestsAndResponses, Duration notBeforeIssuedAtExpiresAtLeewaySeconds)
	{
		this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
		this.discoveryPath = Objects.requireNonNull(discoveryPath, "discoveryPath");
		this.notBeforeIssuedAtExpiresAtLeewaySeconds = Objects.requireNonNull(notBeforeIssuedAtExpiresAtLeewaySeconds,
				"notBeforeIssuedAtExpiresAtLeewaySeconds");

		Objects.requireNonNull(clientId, "clientId");
		Objects.requireNonNull(clientSecret, "clientSecret");

		basicAuthorizationValue = Base64.getEncoder().encodeToString(new StringBuilder().append(clientId).append(':')
				.append(clientSecret).toString().getBytes(StandardCharsets.US_ASCII));

		SSLContext sslContext = null;
		if (trustStore != null && keyStore == null && keyStorePassword == null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).createSSLContext();
		else if (trustStore != null && keyStore != null && keyStorePassword != null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).keyStore(keyStore)
					.keyStorePassword(keyStorePassword).createSSLContext();

		ClientBuilder builder = ClientBuilder.newBuilder();

		if (sslContext != null)
			builder = builder.sslContext(sslContext);

		ClientConfig config = new ClientConfig();
		config.connectorProvider(new ApacheConnectorProvider());
		config.property(ClientProperties.PROXY_URI, proxySchemeHostPort);
		config.property(ClientProperties.PROXY_USERNAME, proxyUserName);
		config.property(ClientProperties.PROXY_PASSWORD, proxyPassword == null ? null : String.valueOf(proxyPassword));
		builder = builder.withConfig(config);

		if (userAgentValue != null && !userAgentValue.isBlank())
			builder = builder.register((ClientRequestFilter) requestContext -> requestContext.getHeaders()
					.add(HttpHeaders.USER_AGENT, userAgentValue));

		builder = builder.connectTimeout(connectTimeout.toMillis(), TimeUnit.MILLISECONDS)
				.readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS);

		if (logRequestsAndResponses)
		{
			builder = builder.register(new LoggingFeature(requestDebugLogger, Level.INFO, Verbosity.PAYLOAD_ANY,
					LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
		}

		client = builder.build();
	}

	private void logUnexpectedResponseAndClose(Response response)
	{
		String message = response.readEntity(String.class);
		logger.debug("Unexpected response, status: {} {}, message: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase(), message);
	}

	/**
	 * @return OIDC {@link Configuration} resource
	 * @throws OidcClientException
	 *             if response status not {@link Status#OK}, response issuer not matching base-url or response supported
	 *             grant types does not include <code>"client_credentials"</code>
	 */
	@Override
	public ConfigurationImpl getConfiguration() throws OidcClientException
	{
		Response response = client.target(baseUrl).path(discoveryPath).request(MediaType.APPLICATION_JSON_TYPE).get();

		if (response.getStatus() == Status.OK.getStatusCode())
		{
			ConfigurationImpl config = response.readEntity(ConfigurationImpl.class);

			if (baseUrl.equals(config.getIssuer()))
				return config;
			else
				throw new OidcClientException("Invalid response: Issuer does not match base-url (" + config.getIssuer()
						+ " not equal to " + baseUrl + ")");
		}
		else
		{
			logUnexpectedResponseAndClose(response);
			throw new OidcClientException("Unexpected response status code " + response.getStatusInfo().getStatusCode()
					+ " " + response.getStatusInfo().getReasonPhrase());
		}
	}

	/**
	 * @return {@link Jwks} resource
	 * @throws OidcClientException
	 *             if response status not {@link Status#OK}
	 */
	@Override
	public JwksImpl getJwks() throws OidcClientException
	{
		return getJwks(getConfiguration());
	}

	/**
	 * @param configuration
	 *            not <code>null</code>
	 * @return {@link Jwks} resource
	 * @throws OidcClientException
	 *             if response status not {@link Status#OK}
	 */
	@Override
	public JwksImpl getJwks(Configuration configuration) throws OidcClientException
	{
		Objects.requireNonNull(configuration, "configuration");

		Response response = client.target(configuration.getJwksUri()).request(MediaType.APPLICATION_JSON_TYPE).get();

		if (response.getStatus() == Status.OK.getStatusCode())
		{
			JwksImpl jwks = response.readEntity(JwksImpl.class);
			return jwks;
		}
		else
		{
			logUnexpectedResponseAndClose(response);
			throw new OidcClientException("Unexpected response status code " + response.getStatusInfo().getStatusCode()
					+ " " + response.getStatusInfo().getReasonPhrase());
		}
	}

	/**
	 * @return access token
	 */
	@Override
	public DecodedJWT getAccessTokenDecoded() throws OidcClientException
	{
		return getAccessTokenDecoded(getConfiguration(), getJwks());
	}

	/**
	 * @param configuration
	 *            not <code>null</code>
	 * @param jwks
	 *            not <code>null</code>
	 * @return access token
	 * @throws OidcClientException
	 *             if response status not {@link Status#OK}, OIDC provider does not support client credentials grant
	 *             (Keycloak: service accounts roles) or returned access token could not be verified
	 */
	@Override
	public DecodedJWT getAccessTokenDecoded(Configuration configuration, Jwks jwks) throws OidcClientException
	{
		Objects.requireNonNull(configuration, "configuration");
		Objects.requireNonNull(jwks, "jwks");

		if (!configuration.getGrantTypesSupported().contains("client_credentials"))
			throw new OidcClientException(
					"OIDC provider does not support Client Credentials Grant, supported grant types: "
							+ configuration.getGrantTypesSupported());

		Response response = client.target(configuration.getTokenEndpoint()).request(MediaType.APPLICATION_JSON_TYPE)
				.header(HttpHeaders.AUTHORIZATION, "Basic " + basicAuthorizationValue)
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

			Optional<Algorithm> algorithm = toAlgorithm(key.get());
			if (key.isEmpty())
				throw new OidcClientException("Access token key with kid '" + keyId
						+ "' has unsupported type (kty) / algorithm (alg) in JWKS '" + key.get().getKty() + "' / '"
						+ key.get().getAlg() + "'");

			try
			{
				return JWT.require(algorithm.get()).acceptLeeway(notBeforeIssuedAtExpiresAtLeewaySeconds.getSeconds())
						.build().verify(decoded);
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

	private Optional<Algorithm> toAlgorithm(JwksKey jwksKey)
	{
		return Optional.ofNullable(switch (jwksKey.getKty())
		{
			case "RSA":
			{
				final RSAPublicKey key = toRsaPublicKey(jwksKey.getN(), jwksKey.getE());
				final RSAKeyProvider keyProvider = toRsaKeyProvider(key, jwksKey.getKid());

				yield switch (jwksKey.getAlg())
				{
					case "RS256" -> Algorithm.RSA256(keyProvider);
					case "RS384" -> Algorithm.RSA384(keyProvider);
					case "RS512" -> Algorithm.RSA512(keyProvider);

					default -> {
						logger.info("JWKS alg property value '" + jwksKey.getAlg()
								+ "' not one of 'RSA256', 'RSA384' or 'RSA512'");
						yield null;
					}
				};
			}
			case "EC":
			{
				final ECPublicKey key = toEcPublicKey(jwksKey.getX(), jwksKey.getY(), jwksKey.getCrv());
				final ECDSAKeyProvider keyProvider = toEcKeyProvider(key, jwksKey.getKid());

				yield switch (jwksKey.getAlg())
				{
					case "ES256" -> Algorithm.ECDSA256(keyProvider);
					case "ES384" -> Algorithm.ECDSA384(keyProvider);
					case "ES512" -> Algorithm.ECDSA512(keyProvider);

					default -> {
						logger.info("JWKS crv property value '" + jwksKey.getAlg()
								+ "' not one of 'ES256', 'ES384' or 'ES512'");
						yield null;
					}
				};
			}
			default:
			{
				logger.info("JWKS kty property '" + jwksKey.getKty() + "' not supported");
				yield null;
			}
		});
	}

	private RSAKeyProvider toRsaKeyProvider(RSAPublicKey key, String kid)
	{
		RSAKeyProvider keyProvider = new RSAKeyProvider()
		{
			@Override
			public RSAPublicKey getPublicKeyById(String keyId)
			{
				if (kid != null && kid.equals(keyId))
					return key;
				else
					return null;
			}

			@Override
			public String getPrivateKeyId()
			{
				return null;
			}

			@Override
			public RSAPrivateKey getPrivateKey()
			{
				return null;
			}
		};
		return keyProvider;
	}

	private RSAPublicKey toRsaPublicKey(String n, String e)
	{
		BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
		BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));

		try
		{
			RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);

			KeyFactory factory = KeyFactory.getInstance("RSA");
			return (RSAPublicKey) factory.generatePublic(keySpec);
		}
		catch (InvalidKeySpecException | NoSuchAlgorithmException ex)
		{
			throw new OidcClientException("Unable to create RSA public key", ex);
		}
	}

	private ECDSAKeyProvider toEcKeyProvider(ECPublicKey key, String kid)
	{
		return new ECDSAKeyProvider()
		{
			@Override
			public ECPublicKey getPublicKeyById(String keyId)
			{
				if (kid != null && kid.equals(keyId))
					return key;
				else
					return null;
			}

			@Override
			public String getPrivateKeyId()
			{
				return null;
			}

			@Override
			public ECPrivateKey getPrivateKey()
			{
				return null;
			}
		};
	}

	private ECPublicKey toEcPublicKey(String x, String y, String crv)
	{
		BigInteger xCoordinate = new BigInteger(1, Base64.getUrlDecoder().decode(x));
		BigInteger yCoordinate = new BigInteger(1, Base64.getUrlDecoder().decode(y));
		ECGenParameterSpec curve = toParameterSpec(crv);

		try
		{
			AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
			parameters.init(curve);
			ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
			ECPublicKeySpec keySpec = new ECPublicKeySpec(new ECPoint(xCoordinate, yCoordinate), ecParameters);

			KeyFactory factory = KeyFactory.getInstance("EC");
			return (ECPublicKey) factory.generatePublic(keySpec);
		}
		catch (NoSuchAlgorithmException | InvalidParameterSpecException | InvalidKeySpecException ex)
		{
			throw new OidcClientException("Unable to create EC public key", ex);
		}
	}

	private ECGenParameterSpec toParameterSpec(String crv)
	{
		return switch (crv)
		{
			case "P-256" -> new ECGenParameterSpec("secp256r1");
			case "P-384" -> new ECGenParameterSpec("secp384r1");
			case "P-521" -> new ECGenParameterSpec("secp521r1");

			default -> {
				logger.info("JWKS crv property value '" + crv + "' not one of 'P-256', 'P-384' or 'P-512'");
				throw new OidcClientException("JWKS crv property value '" + crv + "' not supported");
			}
		};
	}
}