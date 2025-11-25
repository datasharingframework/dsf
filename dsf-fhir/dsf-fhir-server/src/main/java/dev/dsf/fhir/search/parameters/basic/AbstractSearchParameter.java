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
package dev.dsf.fhir.search.parameters.basic;

import java.util.List;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQuerySortParameterConfiguration;
import dev.dsf.fhir.search.SearchQuerySortParameterConfiguration.SortDirection;
import dev.dsf.fhir.search.parameters.SearchQuerySortParameter;

public abstract class AbstractSearchParameter<R extends Resource>
		implements SearchQueryParameter<R>, SearchQuerySortParameter
{
	protected final Class<R> resourceType;
	protected final String parameterName;

	public AbstractSearchParameter(Class<R> resourceType, String parameterName)
	{
		this.resourceType = resourceType;
		this.parameterName = parameterName;
	}

	@Override
	public final String getParameterName()
	{
		return parameterName;
	}

	protected final IllegalStateException notDefined()
	{
		return new IllegalStateException("not defined");
	}

	@Override
	public SearchQueryParameter<R> configure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		doConfigure(errors, queryParameterName, queryParameterValue);
		return this;
	}

	protected abstract void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue);

	@Override
	public SearchQuerySortParameterConfiguration configureSort(List<? super SearchQueryParameterError> errors,
			String queryParameterSortValue)
	{
		SortDirection direction = SortDirection.fromString(queryParameterSortValue);
		return new SearchQuerySortParameterConfiguration(getSortSql(direction.getSqlModifierWithSpacePrefix()),
				parameterName, direction);
	}

	protected abstract String getSortSql(String sortDirectionWithSpacePrefix);

	@Override
	public final boolean matches(Resource resource)
	{
		return resource != null && resourceType.isInstance(resource) && resourceMatches(resourceType.cast(resource));
	}

	protected abstract boolean resourceMatches(R resource);
}
