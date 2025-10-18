package dev.dsf.common.oidc;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class BaseOidcClientWithCache implements BaseOidcClient
{
	private final BaseOidcClient delegate;

	private final AtomicReference<OidcConfiguration> oidcConfiguration = new AtomicReference<>();
	private final AtomicReference<Jwks> jwks = new AtomicReference<>();

	/**
	 * @param delegate
	 *            not <code>null</code>
	 */
	public BaseOidcClientWithCache(BaseOidcClient delegate)
	{
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public OidcConfiguration getConfiguration() throws OidcClientException
	{
		return getOrSet(oidcConfiguration, delegate::getConfiguration);
	}

	private <T> T getOrSet(AtomicReference<T> cache, Supplier<T> supplier)
	{
		T cached = cache.get();
		if (cached == null)
		{
			T value = supplier.get();
			if (cache.compareAndSet(cached, value))
				return value;
			else
				return cache.get();
		}
		else
			return cached;
	}

	@Override
	public Jwks getJwks() throws OidcClientException
	{
		return getOrSet(jwks, () -> delegate.getJwks(getConfiguration()));
	}

	@Override
	public Jwks getJwks(OidcConfiguration configuration) throws OidcClientException
	{
		// ignoring parameter and using cached value
		return getJwks();
	}
}
