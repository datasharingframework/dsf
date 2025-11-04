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
