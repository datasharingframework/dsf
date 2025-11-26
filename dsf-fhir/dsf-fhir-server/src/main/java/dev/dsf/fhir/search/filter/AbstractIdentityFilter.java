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
package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.search.SearchQueryIdentityFilter;

abstract class AbstractIdentityFilter implements SearchQueryIdentityFilter
{
	protected final Identity identity;
	protected final String resourceTable;
	protected final String resourceIdColumn;

	AbstractIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		this.identity = identity;
		this.resourceTable = resourceTable;
		this.resourceIdColumn = resourceIdColumn;
	}
}
