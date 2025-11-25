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
package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.DomainResource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

class ReadByUrlDaoJdbc<R extends DomainResource>
{
	private final Supplier<DataSource> dataSourceSupplier;
	private final BiFunctionWithSqlException<ResultSet, Integer, R> resourceExtractor;

	private final String resourceTable;
	private final String resourceColumn;

	ReadByUrlDaoJdbc(Supplier<DataSource> dataSourceSupplier,
			BiFunctionWithSqlException<ResultSet, Integer, R> resourceExtractor, String resourceTable,
			String resourceColumn)
	{
		this.dataSourceSupplier = dataSourceSupplier;
		this.resourceExtractor = resourceExtractor;
		this.resourceTable = resourceTable;
		this.resourceColumn = resourceColumn;
	}

	/**
	 * @param urlAndVersion
	 *            not <code>null</code>, url|version
	 * @return {@link Optional#empty()} if param <code>urlAndVersion</code> is null or {@link String#isBlank()}
	 * @throws SQLException
	 *             if database access errors occur
	 */
	Optional<R> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		try (Connection connection = dataSourceSupplier.get().getConnection())
		{
			return readByUrlAndVersionWithTransaction(connection, urlAndVersion);
		}
	}

	/**
	 * @param connection
	 *            not <code>null</code>
	 * @param urlAndVersion
	 *            not <code>null</code>, url|version
	 * @return {@link Optional#empty()} if param <code>urlAndVersion</code> is null or {@link String#isBlank()}
	 * @throws SQLException
	 *             if database access errors occur
	 */
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

	/**
	 * @param url
	 *            not <code>null</code>
	 * @param version
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if param <code>url</code> is null or {@link String#isBlank()}
	 * @throws SQLException
	 *             if database access errors occur
	 */
	Optional<R> readByUrlAndVersion(String url, String version) throws SQLException
	{
		try (Connection connection = dataSourceSupplier.get().getConnection())
		{
			return readByUrlAndVersionWithTransaction(connection, url, version);
		}
	}

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
			throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		if (url == null || url.isBlank())
			return Optional.empty();

		String versionSql = version != null && !version.isBlank() ? " AND " + resourceColumn + "->>'version' = ?" : "";
		String sql = "SELECT " + resourceColumn + " FROM current_" + resourceTable + " WHERE " + resourceColumn
				+ "->>'url' = ?" + versionSql;

		try (PreparedStatement statement = connection.prepareStatement(sql))
		{
			statement.setString(1, url);
			if (version != null && !version.isBlank())
				statement.setString(2, version);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
					return Optional.of(resourceExtractor.apply(result, 1));
				else
					return Optional.empty();
			}
		}
	}
}
