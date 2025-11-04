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
package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;

@SearchParameterDefinition(name = OrganizationAffiliationRole.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/OrganizationAffiliation-role", type = SearchParamType.TOKEN, documentation = "Definition of the role the participatingOrganization plays")
public class OrganizationAffiliationRole extends AbstractTokenParameter<OrganizationAffiliation>
{
	public static final String PARAMETER_NAME = "role";

	public OrganizationAffiliationRole()
	{
		super(OrganizationAffiliation.class, PARAMETER_NAME);
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM ->
				"(SELECT jsonb_agg(coding) FROM jsonb_array_elements(organization_affiliation->'code') AS code, jsonb_array_elements(code->'coding') AS coding) @> ?::jsonb";
			case CODE_AND_NO_SYSTEM_PROPERTY ->
				"(SELECT COUNT(*) FROM jsonb_array_elements(organization_affiliation->'code') AS code, jsonb_array_elements(code->'coding') AS coding "
						+ "WHERE coding->>'code' = ? AND NOT (coding ?? 'system')) > 0";
		};
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM ->
				"NOT ((SELECT jsonb_agg(coding) FROM jsonb_array_elements(organization_affiliation->'code') AS code, jsonb_array_elements(code->'coding') AS coding) @> ?::jsonb)";
			case CODE_AND_NO_SYSTEM_PROPERTY ->
				"(SELECT COUNT(*) FROM jsonb_array_elements(organization_affiliation->'code') AS code, jsonb_array_elements(code->'coding') AS coding "
						+ "WHERE coding->>'code' <> ? OR (coding ?? 'system')) > 0";
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
				statement.setString(parameterIndex, "[{\"code\": \"" + valueAndType.codeValue + "\"}]");
				return;

			case CODE_AND_SYSTEM:
				statement.setString(parameterIndex, "[{\"code\": \"" + valueAndType.codeValue + "\", \"system\": \""
						+ valueAndType.systemValue + "\"}]");
				return;

			case CODE_AND_NO_SYSTEM_PROPERTY:
				statement.setString(parameterIndex, valueAndType.codeValue);
				return;

			case SYSTEM:
				statement.setString(parameterIndex, "[{\"system\": \"" + valueAndType.systemValue + "\"}]");
				return;
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(SELECT string_agg((coding->>'system')::text || (coding->>'code')::text, ' ') FROM jsonb_array_elements(organization_affiliation->'code'->'coding') coding)"
				+ sortDirectionWithSpacePrefix;
	}

	@Override
	protected boolean resourceMatches(OrganizationAffiliation resource)
	{
		return resource.hasCode() && codingMatches(resource.getCode());
	}
}
