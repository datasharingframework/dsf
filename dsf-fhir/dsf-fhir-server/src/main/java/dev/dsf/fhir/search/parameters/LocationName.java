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
import org.hl7.fhir.r4.model.Location;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameOrAliasParameter;

@SearchParameterDefinition(name = AbstractNameOrAliasParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Location-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the library")
public class LocationName extends AbstractNameOrAliasParameter<Location>
{
	public LocationName()
	{
		super(Location.class, "location", Location::hasName, Location::getName, Location::hasAlias, Location::getAlias);
	}
}
