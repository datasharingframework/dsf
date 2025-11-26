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
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = ValueSetDate.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-date", type = SearchParamType.DATE, documentation = "The value set publication date")
public class ValueSetDate extends AbstractDateTimeParameter<ValueSet>
{
	public static final String PARAMETER_NAME = "date";

	public ValueSetDate()
	{
		super(ValueSet.class, PARAMETER_NAME, "value_set->>'date'",
				fromDateTime(ValueSet::hasDateElement, ValueSet::getDateElement));
	}
}
