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

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Subscription;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStringParameter;

@SearchParameterDefinition(name = SubscriptionCriteria.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Subscription-criteria", type = SearchParamType.STRING, documentation = "The search rules used to determine when to send a notification (always matches exact)")
public class SubscriptionCriteria extends AbstractStringParameter<Subscription>
{
	public static final String PARAMETER_NAME = "criteria";

	public SubscriptionCriteria()
	{
		super(Subscription.class, PARAMETER_NAME);
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case STARTS_WITH, CONTAINS -> "lower(subscription->>'criteria') LIKE ?";
			case EXACT -> "subscription->>'criteria' = ?";
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
			case STARTS_WITH:
				statement.setString(parameterIndex, valueAndType.value.toLowerCase() + "%");
				return;

			case CONTAINS:
				statement.setString(parameterIndex, "%" + valueAndType.value.toLowerCase() + "%");
				return;

			case EXACT:
				statement.setString(parameterIndex, valueAndType.value);
				return;
		}
	}

	@Override
	protected boolean resourceMatches(Subscription resource)
	{
		return resource.hasCriteria() && criteriaMatches(resource.getCriteria());
	}

	private boolean criteriaMatches(String criteria)
	{
		return switch (valueAndType.type)
		{
			case STARTS_WITH -> criteria.toLowerCase().startsWith(valueAndType.value.toLowerCase());
			case CONTAINS -> criteria.toLowerCase().contains(valueAndType.value.toLowerCase());
			case EXACT -> Objects.equals(criteria, valueAndType.value);
		};
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "subscription->>'criteria'" + sortDirectionWithSpacePrefix;
	}
}
