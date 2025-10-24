package dev.dsf.bpe.client.oidc;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import com.auth0.jwt.interfaces.DecodedJWT;

import dev.dsf.bpe.api.client.oidc.Configuration;
import dev.dsf.bpe.api.client.oidc.Jwks;
import dev.dsf.bpe.api.client.oidc.OidcClientException;

public class OidcClientWithCache implements OidcClientWithDecodedJwt
{
	private static final record CacheEntry<T>(ZonedDateTime timeout, T resource)
	{
	}

	private final Duration cacheTimeoutconfigurationResource;
	private final Duration cacheTimeoutJwksResource;
	private final Duration cacheTimeoutAccessTokenBeforeExpiration;
	private final OidcClientWithDecodedJwt delegate;

	private CacheEntry<Configuration> configurationCache;
	private CacheEntry<Jwks> jwksCache;
	private CacheEntry<DecodedJWT> accessTokenCache;

	/**
	 * @param cacheTimeoutconfigurationResource
	 *            not <code>null</code>, not negative
	 * @param cacheTimeoutJwksResource
	 *            not <code>null</code>, not negative
	 * @param cacheTimeoutAccessTokenBeforeExpiration
	 *            not <code>null</code>, not negative
	 * @param delegate
	 *            not <code>null</code>
	 */
	public OidcClientWithCache(Duration cacheTimeoutconfigurationResource, Duration cacheTimeoutJwksResource,
			Duration cacheTimeoutAccessTokenBeforeExpiration, OidcClientWithDecodedJwt delegate)
	{
		this.cacheTimeoutconfigurationResource = Objects.requireNonNull(cacheTimeoutconfigurationResource,
				"cacheTimeoutconfigurationResource");
		if (cacheTimeoutconfigurationResource.isNegative())
			throw new IllegalArgumentException("cacheTimeoutconfigurationResource negative");

		this.cacheTimeoutJwksResource = Objects.requireNonNull(cacheTimeoutJwksResource, "cacheTimeoutJwksResource");
		if (cacheTimeoutJwksResource.isNegative())
			throw new IllegalArgumentException("cacheTimeoutJwksResource negative");

		this.cacheTimeoutAccessTokenBeforeExpiration = cacheTimeoutAccessTokenBeforeExpiration;
		if (cacheTimeoutAccessTokenBeforeExpiration.isNegative())
			throw new IllegalArgumentException("cacheTimeoutAccessTokenBeforeExpiration negative");

		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public Configuration getConfiguration() throws OidcClientException
	{
		if (configurationCache != null && configurationCache.timeout.isBefore(ZonedDateTime.now()))
			return configurationCache.resource;
		else
		{
			Configuration configuration = delegate.getConfiguration();
			configurationCache = new CacheEntry<Configuration>(
					ZonedDateTime.now().plus(cacheTimeoutconfigurationResource), configuration);
			return configuration;
		}
	}

	@Override
	public Jwks getJwks() throws OidcClientException
	{
		Configuration configuration = getConfiguration();

		if (jwksCache != null && jwksCache.timeout.isBefore(ZonedDateTime.now()))
			return jwksCache.resource;
		else
		{
			Jwks jwks = delegate.getJwks(configuration);
			jwksCache = new CacheEntry<Jwks>(ZonedDateTime.now().plus(cacheTimeoutJwksResource), jwks);
			return jwks;
		}
	}

	@Override
	public Jwks getJwks(Configuration configuration) throws OidcClientException
	{
		// ignoring parameter and using cached value
		return getJwks();
	}

	@Override
	public DecodedJWT getAccessTokenDecoded() throws OidcClientException
	{
		return getAccessTokenDecoded(getConfiguration(), getJwks());
	}

	@Override
	public DecodedJWT getAccessTokenDecoded(Configuration configuration, Jwks jwks) throws OidcClientException
	{
		if (accessTokenCache != null && accessTokenCache.timeout.isBefore(ZonedDateTime.now()))
			return accessTokenCache.resource;
		else
		{
			DecodedJWT accessToken = delegate.getAccessTokenDecoded(configuration, jwks);
			ZonedDateTime expiresAt = ZonedDateTime.ofInstant(accessToken.getExpiresAtAsInstant(), ZoneId.of("UTC"));

			accessTokenCache = new CacheEntry<DecodedJWT>(expiresAt.minus(cacheTimeoutAccessTokenBeforeExpiration),
					accessToken);

			return accessToken;
		}
	}
}
