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

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = ResourceLastUpdated.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Resource-lastUpdated", type = SearchParamType.DATE, documentation = "When the resource version last changed")
public class ResourceLastUpdated<R extends Resource> extends AbstractDateTimeParameter<R>
{
	public static final String PARAMETER_NAME = "_lastUpdated";

	public ResourceLastUpdated(Class<R> resourceType, String resourceColumn)
	{
		super(resourceType, PARAMETER_NAME, resourceColumn + "->'meta'->>'lastUpdated'", fromInstant(
				r -> r.hasMeta() && r.getMeta().hasLastUpdatedElement(), r -> r.getMeta().getLastUpdatedElement()));
	}
}
