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
package dev.dsf.fhir.search.parameters.rev.include;

import java.sql.Connection;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.IncludeParts;
import dev.dsf.fhir.search.SearchQueryIncludeParameterConfiguration;
import dev.dsf.fhir.search.SearchQueryRevIncludeParameter;

public abstract class AbstractRevIncludeParameter implements SearchQueryRevIncludeParameter
{
	@Override
	public SearchQueryIncludeParameterConfiguration configureRevInclude(String queryParameterRevIncludeValue)
	{
		IncludeParts includeParts = IncludeParts.fromString(queryParameterRevIncludeValue);
		String revIncludeSql = getRevIncludeSql(includeParts);

		if (revIncludeSql != null)
			return new SearchQueryIncludeParameterConfiguration(revIncludeSql, includeParts,
					(resource, connection) -> modifyRevIncludeResource(includeParts, resource, connection));
		else
			return null;

	}

	protected abstract String getRevIncludeSql(IncludeParts includeParts);

	/**
	 * Use this method to modify the revinclude resources. This method can be used if the resources returned by the
	 * include SQL are not complete and additional content needs to be retrieved from a not included column. For example
	 * the content of a {@link Binary} resource might not be stored in the json column.
	 *
	 * @param includeParts
	 *            not <code>null</code>
	 * @param resource
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 */
	protected abstract void modifyRevIncludeResource(IncludeParts includeParts, Resource resource,
			Connection connection);
}
