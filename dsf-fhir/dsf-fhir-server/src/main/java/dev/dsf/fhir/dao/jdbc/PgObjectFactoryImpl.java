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

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.hl7.fhir.r4.model.Resource;
import org.postgresql.util.PGobject;
import org.springframework.beans.factory.InitializingBean;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;

public class PgObjectFactoryImpl implements PgObjectFactory, InitializingBean
{
	private final FhirContext fhirContext;
	private final ObjectMapper objectMapper;

	public PgObjectFactoryImpl(FhirContext fhirContext, ObjectMapper objectMapper)
	{
		this.fhirContext = fhirContext;
		this.objectMapper = objectMapper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(objectMapper, "objectMapper");
	}

	@Override
	public IParser getJsonParser()
	{
		IParser p = fhirContext.newJsonParser();
		p.setStripVersionsFromReferences(false);
		return p;
	}

	@Override
	public final PGobject resourceToPgObject(Resource resource) throws SQLException
	{
		if (resource == null)
			return null;

		try
		{
			return createPgObjectJsonb(getJsonParser().encodeResourceToString(resource));
		}
		catch (DataFormatException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public PGobject jsonParameterToPgObject(JsonParameter parameter) throws SQLException
	{
		if (parameter == null)
			return null;

		try
		{
			return createPgObjectJsonb(objectMapper.writeValueAsString(parameter));
		}
		catch (JsonProcessingException e)
		{
			throw new SQLException(e);
		}
	}

	@Override
	public PGobject jsonParameterToPgObjectAsArray(JsonParameter... parameter) throws SQLException
	{
		if (parameter == null)
			return null;

		try
		{
			return createPgObjectJsonb(objectMapper.writeValueAsString(parameter));
		}
		catch (JsonProcessingException e)
		{
			throw new SQLException(e);
		}
	}

	private PGobject createPgObjectJsonb(String value) throws SQLException
	{
		PGobject o = new PGobject();
		o.setType("JSONB");
		o.setValue(value);
		return o;
	}

	@Override
	public final PGobject uuidToPgObject(UUID uuid) throws SQLException
	{
		if (uuid == null)
			return null;

		return createPgObjectUuid(uuid.toString());
	}

	private PGobject createPgObjectUuid(String value) throws SQLException
	{
		PGobject o = new PGobject();
		o.setType("UUID");
		o.setValue(value);
		return o;
	}
}
