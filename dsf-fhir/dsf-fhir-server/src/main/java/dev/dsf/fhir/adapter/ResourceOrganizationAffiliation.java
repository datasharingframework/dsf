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

import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

public class ResourceOrganizationAffiliation extends AbstractResource<OrganizationAffiliation>
{
	private record Element(List<ElementSystemValue> identifier, ElementId organization,
			ElementId participatingOrganization, List<ElementSystemValue> code, ElementId endpoint)
	{
	}

	public ResourceOrganizationAffiliation()
	{
		super(OrganizationAffiliation.class, ActiveOrStatus.active(OrganizationAffiliation::hasActiveElement,
				OrganizationAffiliation::getActiveElement));
	}

	@Override
	protected Element toElement(OrganizationAffiliation resource)
	{
		List<ElementSystemValue> identifier = getIdentifiers(resource, OrganizationAffiliation::hasIdentifier,
				OrganizationAffiliation::getIdentifier);

		ElementId organization = ElementId.from(resource, OrganizationAffiliation::hasOrganization,
				OrganizationAffiliation::getOrganization);

		ElementId participatingOrganization = ElementId.from(resource,
				OrganizationAffiliation::hasParticipatingOrganization,
				OrganizationAffiliation::getParticipatingOrganization);

		List<ElementSystemValue> code = resource.hasCode()
				? resource.getCode().stream().map(CodeableConcept::getCoding).flatMap(List::stream)
						.map(ElementSystemValue::from).toList()
				: null;

		ElementId endpoint = ElementId.from(resource, OrganizationAffiliation::hasEndpoint,
				OrganizationAffiliation::getEndpointFirstRep);

		return new Element(nullIfEmpty(identifier), organization, participatingOrganization, nullIfEmpty(code),
				endpoint);
	}
}
