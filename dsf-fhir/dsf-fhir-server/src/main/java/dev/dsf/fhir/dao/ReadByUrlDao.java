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
package dev.dsf.fhir.dao;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.MetadataResource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public interface ReadByUrlDao<R extends MetadataResource>
{
	@FunctionalInterface
	public interface ReadByUrlDaoFactory<R extends MetadataResource>
	{
		ReadByUrlDao<R> create(Supplier<DataSource> dataSourceSupplier,
				BiFunctionWithSqlException<ResultSet, Integer, R> resourceExtractor, String resourceTable,
				String resourceColumn);
	}

	/**
	 * @param urlAndVersion
	 *            not <code>null</code>, url|version
	 * @return {@link Optional#empty()} if param <code>urlAndVersion</code> is null or {@link String#isBlank()}
	 * @throws SQLException
	 *             if database access errors occur
	 */
	Optional<R> readByUrlAndVersion(String urlAndVersion) throws SQLException;

	/**
	 * @param url
	 *            not <code>null</code>
	 * @param version
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if param <code>url</code> is null or {@link String#isBlank()}
	 * @throws SQLException
	 *             if database access errors occur
	 */
	Optional<R> readByUrlAndVersion(String url, String version) throws SQLException;

	/**
	 * @param connection
	 *            not <code>null</code>
	 * @param urlAndVersion
	 *            not <code>null</code>, url|version
	 * @return {@link Optional#empty()} if param <code>urlAndVersion</code> is null or {@link String#isBlank()}
	 * @throws SQLException
	 *             if database access errors occur
	 */
	Optional<R> readByUrlAndVersionWithTransaction(Connection connection, String urlAndVersion) throws SQLException;

	/**
	 * @param connection
	 *            not <code>null</code>
	 * @param url
	 *            not <code>null</code>
	 * @param version
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if param <code>url</code> is null or {@link String#isBlank()}
	 * @throws SQLException
	 *             if database access errors occur
	 */
	Optional<R> readByUrlAndVersionWithTransaction(Connection connection, String url, String version)
			throws SQLException;

	void onResourceCreated(R resource);
}
