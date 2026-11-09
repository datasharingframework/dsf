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
package dev.dsf.fhir.dao.cache;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r4.model.MetadataResource;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import dev.dsf.fhir.dao.ReadByUrlDao;

public class ReadByUrlDaoNotFoundCache<R extends MetadataResource> implements ReadByUrlDao<R>
{
	private final static record UrlAndVersion(String url, String version)
	{
	}

	private final Cache<UrlAndVersion, Boolean> notFoundCache = Caffeine.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES).maximumSize(10_000).build();

	private final ReadByUrlDao<R> delegate;

	public ReadByUrlDaoNotFoundCache(ReadByUrlDao<R> delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public Optional<R> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		if (urlAndVersion == null || urlAndVersion.isBlank())
			return Optional.empty();

		String[] split = urlAndVersion.split("[|]");
		if (split.length < 1 || split.length > 2)
			return Optional.empty();

		return readByUrlAndVersion(split[0], split.length == 2 ? split[1] : null);
	}

	@Override
	public Optional<R> readByUrlAndVersion(String url, String version) throws SQLException
	{
		if (Boolean.TRUE.equals(notFoundCache.getIfPresent(new UrlAndVersion(url, version))))
			return Optional.empty();

		Optional<R> r = delegate.readByUrlAndVersion(url, version);

		if (r.isEmpty())
			notFoundCache.put(new UrlAndVersion(url, version), Boolean.TRUE);

		return r;
	}

	@Override
	public Optional<R> readByUrlAndVersionWithTransaction(Connection connection, String urlAndVersion)
			throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		if (urlAndVersion == null || urlAndVersion.isBlank())
			return Optional.empty();

		String[] split = urlAndVersion.split("[|]");
		if (split.length < 1 || split.length > 2)
			return Optional.empty();

		return readByUrlAndVersionWithTransaction(connection, split[0], split.length == 2 ? split[1] : null);
	}

	@Override
	public Optional<R> readByUrlAndVersionWithTransaction(Connection connection, String url, String version)
			throws SQLException
	{
		if (Boolean.TRUE.equals(notFoundCache.getIfPresent(new UrlAndVersion(url, version))))
			return Optional.empty();

		Optional<R> r = delegate.readByUrlAndVersionWithTransaction(connection, url, version);

		if (r.isEmpty())
			notFoundCache.put(new UrlAndVersion(url, version), Boolean.TRUE);

		return r;
	}

	@Override
	public void onResourceCreated(R resource)
	{
		notFoundCache.invalidate(new UrlAndVersion(resource.getUrl(), null));
		notFoundCache.invalidate(new UrlAndVersion(resource.getUrl(), resource.getVersion()));
	}
}
