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

import java.util.Objects;

import org.hl7.fhir.r4.model.Resource;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;

abstract class AbstractPreparedStatementFactory<R extends Resource> extends PgObjectFactoryImpl
		implements PreparedStatementFactory<R>
{
	private final Class<R> resourceType;

	private final String createSql;
	private final String readByIdSql;
	private final String readByIdAndVersionSql;
	private final String updateSql;

	protected AbstractPreparedStatementFactory(FhirContext fhirContext, ObjectMapper objectMapper,
			Class<R> resourceType, String createSql, String readByIdSql, String readByIdAndVersionSql, String updateSql)
	{
		super(fhirContext, objectMapper);

		this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
		this.createSql = Objects.requireNonNull(createSql, "createSql");
		this.readByIdSql = Objects.requireNonNull(readByIdSql, "readByIdSql");
		this.readByIdAndVersionSql = Objects.requireNonNull(readByIdAndVersionSql, "readByIdAndVersionSql");
		this.updateSql = Objects.requireNonNull(updateSql, "updateSql");
	}

	protected final R jsonToResource(String json)
	{
		return getJsonParser().parseResource(resourceType, json);
	}

	@Override
	public final String getCreateSql()
	{
		return createSql;
	}

	@Override
	public final String getReadByIdSql()
	{
		return readByIdSql;
	}

	@Override
	public final String getReadByIdAndVersionSql()
	{
		return readByIdAndVersionSql;
	}

	@Override
	public final String getUpdateSql()
	{
		return updateSql;
	}
}
