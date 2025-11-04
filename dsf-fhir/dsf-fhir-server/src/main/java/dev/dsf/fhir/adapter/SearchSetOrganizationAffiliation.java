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

import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

public class SearchSetOrganizationAffiliation extends AbstractSearchSet<OrganizationAffiliation>
{
	private record Row(ElementId id, boolean active, ElementId parentOrganization, ElementId participatingOrganization,
			String role, ElementId endpoint, String lastUpdated)
	{
	}

	public SearchSetOrganizationAffiliation(int defaultPageCount)
	{
		super(defaultPageCount, OrganizationAffiliation.class);
	}

	@Override
	protected Row toRow(ElementId id, OrganizationAffiliation resource)
	{
		boolean active = resource.hasActiveElement() && resource.getActiveElement().hasValue()
				&& Boolean.TRUE.equals(resource.getActiveElement().getValue());

		ElementId parentOrganization = ElementId.from(resource, OrganizationAffiliation::hasOrganization,
				OrganizationAffiliation::getOrganization);
		ElementId participatingOrganization = ElementId.from(resource,
				OrganizationAffiliation::hasParticipatingOrganization,
				OrganizationAffiliation::getParticipatingOrganization);

		String role = resource.getCode().stream().flatMap(c -> c.getCoding().stream())
				.filter(c -> CODE_SYSTEM_ORGANIZATION_ROLE.equals(c.getSystem())).map(Coding::getCode)
				.collect(Collectors.joining(", "));

		ElementId endpoint = ElementId.from(resource, OrganizationAffiliation::hasEndpoint,
				OrganizationAffiliation::getEndpointFirstRep);

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, active, parentOrganization, participatingOrganization, role, endpoint, lastUpdated);
	}
}
