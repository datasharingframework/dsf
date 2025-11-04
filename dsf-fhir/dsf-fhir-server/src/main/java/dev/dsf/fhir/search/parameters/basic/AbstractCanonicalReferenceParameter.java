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

import org.hl7.fhir.r4.model.DomainResource;

import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;

public abstract class AbstractCanonicalReferenceParameter<R extends DomainResource>
		extends AbstractReferenceParameter<R>
{
	public AbstractCanonicalReferenceParameter(Class<R> resourceType, String parameterName,
			String... targetResourceTypeNames)
	{
		super(resourceType, parameterName, targetResourceTypeNames);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type != null)
			switch (valueAndType.type)
			{
				// only URL supported for canonical
				case URL:
					return;

				case ID:
				case TYPE_AND_ID:
				case IDENTIFIER:
				case RESOURCE_NAME_AND_ID:
				case TYPE_AND_RESOURCE_NAME_AND_ID:
				default:
					errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							parameterName, queryParameterValue));
					return;
			}
	}
}
