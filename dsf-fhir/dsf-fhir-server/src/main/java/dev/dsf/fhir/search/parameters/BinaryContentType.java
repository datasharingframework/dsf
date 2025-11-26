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

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;
import dev.dsf.fhir.search.parameters.basic.TokenSearchType;

@SearchParameterDefinition(name = BinaryContentType.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Binary-contentType", type = SearchParamType.TOKEN, documentation = "The MIME type of the actual binary content")
public class BinaryContentType extends AbstractTokenParameter<Binary>
{
	public static final String PARAMETER_NAME = "contentType";

	private CodeType contentType;

	public BinaryContentType()
	{
		super(Binary.class, PARAMETER_NAME);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type == TokenSearchType.CODE)
			contentType = toContentType(valueAndType.codeValue);
	}

	private CodeType toContentType(String contentType)
	{
		if (contentType == null || contentType.isBlank())
			return null;

		return new CodeType(contentType);
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && contentType != null;
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return "binary_json->>'contentType' = ?";
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return "binary_json->>'contentType' <> ?";
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
		statement.setString(parameterIndex, contentType.getValue());
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return contentType.getValue();
	}

	@Override
	protected boolean resourceMatches(Binary resource)
	{
		return valueAndType.negated ^ resource.hasContentType()
				&& resource.getContentType().equals(contentType.getValue());
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "binary_json->>'contentType'" + sortDirectionWithSpacePrefix;
	}
}
