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
package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.Organization;

public class SearchSetOrganization extends AbstractSearchSet<Organization>
{
	private record Row(ElementId id, boolean active, String identifier, String name, ElementId endpoint,
			int endpointCount, String lastUpdated)
	{
	}

	public SearchSetOrganization(int defaultPageCount)
	{
		super(defaultPageCount, Organization.class);
	}

	@Override
	protected Row toRow(ElementId id, Organization resource)
	{
		boolean active = resource.hasActiveElement() && resource.getActiveElement().hasValue()
				&& Boolean.TRUE.equals(resource.getActiveElement().getValue());

		String identifier = getIdentifierValues(resource, Organization::hasIdentifier, Organization::getIdentifier,
				NAMING_SYSTEM_ORGANIZATION_IDENTIFIER);
		String name = resource.hasName() ? resource.getName() : "";

		ElementId endpoint = ElementId.from(resource, Organization::hasEndpoint, Organization::getEndpointFirstRep);
		int endpointCount = resource.hasEndpoint() ? resource.getEndpoint().size() : 0;

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, active, identifier, name, endpoint, endpointCount, lastUpdated);
	}
}
