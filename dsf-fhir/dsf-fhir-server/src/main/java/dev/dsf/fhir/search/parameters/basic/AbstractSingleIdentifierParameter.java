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
package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.BiPredicate;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public class AbstractSingleIdentifierParameter<R extends Resource> extends AbstractIdentifierParameter<R>
{
	public AbstractSingleIdentifierParameter(Class<R> resourceType, String resourceColumn,
			BiPredicate<TokenValueAndSearchType, R> identifierMatches)
	{
		super(resourceType, resourceColumn, identifierMatches);
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM -> resourceColumn + "->'identifier' = ?::jsonb";
			case CODE_AND_NO_SYSTEM_PROPERTY -> resourceColumn + "->'identifier'->>'value' = ? AND NOT ("
					+ resourceColumn + "->'identifier' ?? 'system')";
		};
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM -> resourceColumn + "->'identifier' <> ?::jsonb";
			case CODE_AND_NO_SYSTEM_PROPERTY ->
				resourceColumn + "->'identifier'->>'value' <> ? OR (" + resourceColumn + "->'identifier' ?? 'system')";
		};
	}

	@Override
	public int getSqlParameterCount()
	{
		return 1;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException
	{
		switch (valueAndType.type)
		{
			case CODE:
				statement.setString(parameterIndex, "{\"value\": \"" + valueAndType.codeValue + "\"}");
				return;

			case CODE_AND_SYSTEM:
				statement.setString(parameterIndex, "{\"value\": \"" + valueAndType.codeValue + "\", \"system\": \""
						+ valueAndType.systemValue + "\"}");
				return;

			case CODE_AND_NO_SYSTEM_PROPERTY:
				statement.setString(parameterIndex, valueAndType.codeValue);
				return;

			case SYSTEM:
				statement.setString(parameterIndex, "{\"system\": \"" + valueAndType.systemValue + "\"}");
				return;
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(" + resourceColumn + "->'identifier'->>'system')::text || (" + resourceColumn
				+ "->'identifier'->>'value')::text" + sortDirectionWithSpacePrefix;
	}
}
