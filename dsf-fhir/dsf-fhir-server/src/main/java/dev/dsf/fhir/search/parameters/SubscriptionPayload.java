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
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Subscription;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;
import dev.dsf.fhir.search.parameters.basic.TokenSearchType;

@SearchParameterDefinition(name = SubscriptionPayload.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Subscription-payload", type = SearchParamType.TOKEN, documentation = "The mime-type of the notification payload")
public class SubscriptionPayload extends AbstractTokenParameter<Subscription>
{
	public static final String PARAMETER_NAME = "payload";

	private String payloadMimeType;

	public SubscriptionPayload()
	{
		super(Subscription.class, PARAMETER_NAME);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type == TokenSearchType.CODE)
			payloadMimeType = valueAndType.codeValue;
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && payloadMimeType != null;
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return "subscription->'channel'->>'payload' = ?";
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return "subscription->'channel'->>'payload' <> ?";
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
		statement.setString(parameterIndex, payloadMimeType);
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return payloadMimeType;
	}

	@Override
	protected boolean resourceMatches(Subscription resource)
	{
		return valueAndType.negated ^ (resource.hasChannel() && resource.getChannel().hasPayload()
				&& Objects.equals(resource.getChannel().getPayload(), payloadMimeType));
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "subscription->'channel'->>'payload'" + sortDirectionWithSpacePrefix;
	}
}
