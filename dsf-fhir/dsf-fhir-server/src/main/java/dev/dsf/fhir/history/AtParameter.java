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

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

public class AtParameter extends AbstractDateTimeParameter<Resource>
{
	public static final String PARAMETER_NAME = "_at";

	public AtParameter()
	{
		super(Resource.class, PARAMETER_NAME, "last_updated", null);
	}

	@Override
	protected boolean resourceMatches(Resource resource)
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
}
