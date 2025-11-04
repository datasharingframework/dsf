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
package dev.dsf.fhir.history.filter;

import dev.dsf.fhir.search.SearchQueryIdentityFilter;

public interface HistoryIdentityFilter extends SearchQueryIdentityFilter
{
	String RESOURCE_ID_COLUMN = "id";
	String RESOURCE_COLUMN = "resource";
	String RESOURCE_TABLE = "history";

	static String getFilterQuery(String resourceType, String filterQuery)
	{
		if (filterQuery == null || filterQuery.isBlank())
			return "(type = '" + resourceType + "')";
		else
			return "(type = '" + resourceType + "' AND " + filterQuery + ")";
	}

	default boolean isDefined()
	{
		String filterQuery = getFilterQuery();
		return filterQuery != null && !filterQuery.isBlank();
	}
}
