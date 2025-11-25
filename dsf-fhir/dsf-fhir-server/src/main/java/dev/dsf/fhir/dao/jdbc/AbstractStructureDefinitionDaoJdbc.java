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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.search.SearchQueryIdentityFilter;
import dev.dsf.fhir.search.SearchQueryParameter;
import dev.dsf.fhir.search.SearchQueryParameterFactory;
import dev.dsf.fhir.search.parameters.StructureDefinitionDate;
import dev.dsf.fhir.search.parameters.StructureDefinitionIdentifier;
import dev.dsf.fhir.search.parameters.StructureDefinitionName;
import dev.dsf.fhir.search.parameters.StructureDefinitionStatus;
import dev.dsf.fhir.search.parameters.StructureDefinitionUrl;
import dev.dsf.fhir.search.parameters.StructureDefinitionVersion;

abstract class AbstractStructureDefinitionDaoJdbc extends AbstractResourceDaoJdbc<StructureDefinition>
		implements StructureDefinitionDao
{
	private static <R extends Resource> SearchQueryParameterFactory<R> factory(String resourceColumn,
			String parameterName, Function<String, SearchQueryParameter<R>> supplier, List<String> nameModifiers)
	{
		return factory(parameterName, () -> supplier.apply(resourceColumn), nameModifiers);
	}

	private static <R extends Resource> SearchQueryParameterFactory<R> factory(String resourceColumn,
			String parameterName, Function<String, SearchQueryParameter<R>> supplier)
	{
		return factory(parameterName, () -> supplier.apply(resourceColumn));
	}

	private final ReadByUrlDaoJdbc<StructureDefinition> readByUrl;
	private final String readByBaseDefinition;

	protected AbstractStructureDefinitionDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext, String resourceTable, String resourceColumn, String resourceIdColumn,
			Function<Identity, SearchQueryIdentityFilter> userFilter)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, StructureDefinition.class, resourceTable,
				resourceColumn, resourceIdColumn, userFilter,
				List.of(factory(resourceColumn, StructureDefinitionDate.PARAMETER_NAME, StructureDefinitionDate::new),
						factory(resourceColumn, StructureDefinitionIdentifier.PARAMETER_NAME,
								StructureDefinitionIdentifier::new, StructureDefinitionIdentifier.getNameModifiers()),
						factory(resourceColumn, StructureDefinitionName.PARAMETER_NAME, StructureDefinitionName::new,
								StructureDefinitionName.getNameModifiers()),
						factory(resourceColumn, StructureDefinitionStatus.PARAMETER_NAME,
								StructureDefinitionStatus::new, StructureDefinitionStatus.getNameModifiers()),
						factory(resourceColumn, StructureDefinitionUrl.PARAMETER_NAME, StructureDefinitionUrl::new,
								StructureDefinitionUrl.getNameModifiers()),
						factory(resourceColumn, StructureDefinitionVersion.PARAMETER_NAME,
								StructureDefinitionVersion::new, StructureDefinitionVersion.getNameModifiers())),
				List.of());

		readByUrl = new ReadByUrlDaoJdbc<>(this::getDataSource, this::getResource, resourceTable, resourceColumn);

		readByBaseDefinition = "SELECT " + resourceColumn + " FROM current_" + resourceTable + " WHERE "
				+ resourceColumn + "->>'baseDefinition' = ?";
	}

	@Override
	public Optional<StructureDefinition> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(urlAndVersion);
	}

	@Override
	public Optional<StructureDefinition> readByUrlAndVersionWithTransaction(Connection connection, String urlAndVersion)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, urlAndVersion);
	}

	@Override
	public Optional<StructureDefinition> readByUrlAndVersion(String url, String version) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(url, version);
	}

	@Override
	public Optional<StructureDefinition> readByUrlAndVersionWithTransaction(Connection connection, String url,
			String version) throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, url, version);
	}

	@Override
	public List<StructureDefinition> readAllByBaseDefinitionWithTransaction(Connection connection,
			String baseDefinition) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(baseDefinition, "baseDefinition");

		try (PreparedStatement statement = connection.prepareStatement(readByBaseDefinition))
		{
			statement.setString(1, baseDefinition);

			try (ResultSet result = statement.executeQuery())
			{
				List<StructureDefinition> byBaseDefinition = new ArrayList<>();

				while (result.next())
					byBaseDefinition.add(getResource(result, 1));

				return byBaseDefinition;
			}
		}
	}
}
