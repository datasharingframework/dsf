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
package dev.dsf.fhir.history;

import java.util.List;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

public class SinceParameter extends AbstractDateTimeParameter<Resource>
{
	public static final String PARAMETER_NAME = "_since";

	public SinceParameter()
	{
		super(Resource.class, PARAMETER_NAME, "last_updated", null);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (!DateTimeSearchType.EQ.equals(valueAndType.searchType)
				|| !DateTimeType.ZONED_DATE_TIME.equals(valueAndType.type))
		{
			errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE, parameterName,
					queryParameterValue, "Not instant"));
			valueAndType = null;
		}
		else
		{
			valueAndType = new DateTimeValueAndTypeAndSearchType(valueAndType.value, valueAndType.type,
					DateTimeSearchType.GE);
		}
	}

	@Override
	public boolean resourceMatches(Resource resource)
	{
		// Not implemented for history
		throw new UnsupportedOperationException();
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		// Not implemented for history
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return toUrlValue(valueAndType);
	}
}
