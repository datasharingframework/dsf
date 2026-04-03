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

import org.hl7.fhir.r4.model.MetadataResource;

import dev.dsf.fhir.dao.ReadByUrlDao;
import dev.dsf.fhir.function.BiFunctionWithSqlException;

public class ReadByUrlDaoJdbc<R extends MetadataResource> implements ReadByUrlDao<R>
{
	private final Supplier<DataSource> dataSourceSupplier;
	private final BiFunctionWithSqlException<ResultSet, Integer, R> resourceExtractor;

	private final String resourceTable;
	private final String resourceColumn;

	public ReadByUrlDaoJdbc(Supplier<DataSource> dataSourceSupplier,
			BiFunctionWithSqlException<ResultSet, Integer, R> resourceExtractor, String resourceTable,
			String resourceColumn)
	{
		this.dataSourceSupplier = Objects.requireNonNull(dataSourceSupplier, "dataSourceSupplier");
		this.resourceExtractor = Objects.requireNonNull(resourceExtractor, "resourceExtractor");
		this.resourceTable = Objects.requireNonNull(resourceTable, "resourceTable");
		this.resourceColumn = Objects.requireNonNull(resourceColumn, "resourceColumn");
	}

	@Override
	public void onResourceCreated(R resource)
	{
	}

	@Override
	public Optional<R> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		try (Connection connection = dataSourceSupplier.get().getConnection())
		{
			return readByUrlAndVersionWithTransaction(connection, urlAndVersion);
		}
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
	public Optional<R> readByUrlAndVersion(String url, String version) throws SQLException
	{
		try (Connection connection = dataSourceSupplier.get().getConnection())
		{
			return readByUrlAndVersionWithTransaction(connection, url, version);
		}
	}

	@Override
	public Optional<R> readByUrlAndVersionWithTransaction(Connection connection, String url, String version)
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
