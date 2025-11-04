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
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.CodeSystem;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.search.filter.CodeSystemIdentityFilter;
import dev.dsf.fhir.search.parameters.CodeSystemDate;
import dev.dsf.fhir.search.parameters.CodeSystemIdentifier;
import dev.dsf.fhir.search.parameters.CodeSystemName;
import dev.dsf.fhir.search.parameters.CodeSystemStatus;
import dev.dsf.fhir.search.parameters.CodeSystemUrl;
import dev.dsf.fhir.search.parameters.CodeSystemVersion;

public class CodeSystemDaoJdbc extends AbstractResourceDaoJdbc<CodeSystem> implements CodeSystemDao
{
	private final ReadByUrlDaoJdbc<CodeSystem> readByUrl;

	public CodeSystemDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, CodeSystem.class, "code_systems", "code_system",
				"code_system_id", CodeSystemIdentityFilter::new,
				List.of(factory(CodeSystemDate.PARAMETER_NAME, CodeSystemDate::new),
						factory(CodeSystemIdentifier.PARAMETER_NAME, CodeSystemIdentifier::new,
								CodeSystemIdentifier.getNameModifiers()),
						factory(CodeSystemName.PARAMETER_NAME, CodeSystemName::new, CodeSystemName.getNameModifiers()),
						factory(CodeSystemStatus.PARAMETER_NAME, CodeSystemStatus::new,
								CodeSystemStatus.getNameModifiers()),
						factory(CodeSystemUrl.PARAMETER_NAME, CodeSystemUrl::new, CodeSystemUrl.getNameModifiers()),
						factory(CodeSystemVersion.PARAMETER_NAME, CodeSystemVersion::new,
								CodeSystemVersion.getNameModifiers())),
				List.of());

		readByUrl = new ReadByUrlDaoJdbc<>(this::getDataSource, this::getResource, getResourceTable(),
				getResourceColumn());
	}

	@Override
	protected CodeSystem copy(CodeSystem resource)
	{
		return resource.copy();
	}

	@Override
	public Optional<CodeSystem> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(urlAndVersion);
	}

	@Override
	public Optional<CodeSystem> readByUrlAndVersionWithTransaction(Connection connection, String urlAndVersion)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, urlAndVersion);
	}

	@Override
	public Optional<CodeSystem> readByUrlAndVersion(String url, String version) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(url, version);
	}

	@Override
	public Optional<CodeSystem> readByUrlAndVersionWithTransaction(Connection connection, String url, String version)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, url, version);
	}
}
