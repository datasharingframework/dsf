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

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;
import dev.dsf.fhir.search.parameters.basic.TokenSearchType;

@SearchParameterDefinition(name = SubscriptionType.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Subscription-type", type = SearchParamType.TOKEN, documentation = "The type of channel for the sent notifications")
public class SubscriptionType extends AbstractTokenParameter<Subscription>
{
	public static final String PARAMETER_NAME = "type";

	private SubscriptionChannelType channelType;

	public SubscriptionType()
	{
		super(Subscription.class, PARAMETER_NAME);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type == TokenSearchType.CODE)
			channelType = toChannelType(errors, valueAndType.codeValue, queryParameterValue);
	}

	private SubscriptionChannelType toChannelType(List<? super SearchQueryParameterError> errors, String status,
			String queryParameterValue)
	{
		if (status == null || status.isBlank())
			return null;

		try
		{
			return SubscriptionChannelType.fromCode(status);
		}
		catch (FHIRException e)
		{
			errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE, parameterName,
					queryParameterValue, e));
			return null;
		}
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && channelType != null;
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return "subscription->'channel'->>'type' = ?";
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return "subscription->'channel'->>'type' <> ?";
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
		statement.setString(parameterIndex, channelType.toCode());
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return channelType.toCode();
	}

	@Override
	protected boolean resourceMatches(Subscription resource)
	{
		return valueAndType.negated ^ (resource.hasChannel() && resource.getChannel().hasType()
				&& Objects.equals(resource.getChannel().getType(), channelType));
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "subscription->'channel'->>'type'" + sortDirectionWithSpacePrefix;
	}
}
