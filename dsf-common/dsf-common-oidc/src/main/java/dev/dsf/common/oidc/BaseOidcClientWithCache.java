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
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class BaseOidcClientWithCache implements BaseOidcClient
{
	private static final record CacheEntry<T>(ZonedDateTime timeout, T resource)
	{
	}

	private final Duration cacheTimeoutConfigurationResource;
	private final Duration cacheTimeoutJwksResource;

	private final AtomicReference<CacheEntry<OidcConfiguration>> configurationCache = new AtomicReference<>();
	private final AtomicReference<CacheEntry<Jwks>> jwksCache = new AtomicReference<>();

	private final BaseOidcClient delegate;

	/**
	 * @param cacheTimeoutconfigurationResource
	 *            not <code>null</code>
	 * @param cacheTimeoutJwksResource
	 *            not <code>null</code>
	 * @param delegate
	 *            not <code>null</code>
	 */
	public BaseOidcClientWithCache(Duration cacheTimeoutconfigurationResource, Duration cacheTimeoutJwksResource,
			BaseOidcClient delegate)
	{
		this.cacheTimeoutConfigurationResource = Objects.requireNonNull(cacheTimeoutconfigurationResource,
				"cacheTimeoutconfigurationResource");
		this.cacheTimeoutJwksResource = Objects.requireNonNull(cacheTimeoutJwksResource, "cacheTimeoutJwksResource");
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public OidcConfiguration getConfiguration() throws OidcClientException
	{
		return getOrSet(configurationCache, cacheTimeoutConfigurationResource, delegate::getConfiguration);
	}

	private <T> T getOrSet(AtomicReference<CacheEntry<T>> cache, Duration timeout, Supplier<T> supplier)
	{
		CacheEntry<T> cached = cache.get();
		if (cached != null && cached.timeout.isAfter(ZonedDateTime.now()))
			return cached.resource;
		else
		{
			cache.compareAndSet(cached, new CacheEntry<>(ZonedDateTime.now().plus(timeout), supplier.get()));
			return cache.get().resource;
		}
	}

	@Override
	public Jwks getJwks() throws OidcClientException
	{
		return getOrSet(jwksCache, cacheTimeoutJwksResource, () -> delegate.getJwks(getConfiguration()));
	}

	@Override
	public Jwks getJwks(OidcConfiguration configuration) throws OidcClientException
	{
		// ignoring parameter and using cached value
		return getJwks();
	}
}
