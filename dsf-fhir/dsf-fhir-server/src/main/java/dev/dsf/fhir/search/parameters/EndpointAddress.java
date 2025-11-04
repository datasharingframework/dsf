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
import java.util.Objects;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractCanonicalUrlParameter;

@SearchParameterDefinition(name = EndpointAddress.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Endpoint-address", type = SearchParamType.URI, documentation = "The address (url) of the endpoint")
public class EndpointAddress extends AbstractCanonicalUrlParameter<Endpoint>
{
	public static final String PARAMETER_NAME = "address";

	public EndpointAddress()
	{
		super(Endpoint.class, PARAMETER_NAME);
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case PRECISE -> "endpoint->>'address' = ?";
			case BELOW -> "endpoint->>'address' LIKE ?";
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
			case PRECISE:
				statement.setString(parameterIndex, valueAndType.url);
				return;

			case BELOW:
				statement.setString(parameterIndex, valueAndType.url + "%");
				return;
		}
	}

	@Override
	protected boolean resourceMatches(Endpoint resource)
	{
		return resource.hasAddress() && switch (valueAndType.type)
		{
			case PRECISE -> Objects.equals(resource.getAddress(), valueAndType.url);
			case BELOW -> resource.getAddress().startsWith(valueAndType.url);
		};
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "endpoint->>'address'" + sortDirectionWithSpacePrefix;
	}
}
