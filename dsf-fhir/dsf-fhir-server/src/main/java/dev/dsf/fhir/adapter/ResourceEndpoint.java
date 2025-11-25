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

import org.hl7.fhir.r4.model.CodeType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Endpoint;

public class ResourceEndpoint extends AbstractResource<Endpoint>
{
	private record Element(List<ElementSystemValue> identifier, ElementSystemValue connectionType, String name,
			ElementId managingOrganization, List<ElementSystemValue> payloadType, List<String> payloadMimeType,
			String address)
	{
	}

	public ResourceEndpoint()
	{
		super(Endpoint.class, ActiveOrStatus.status(Endpoint::hasStatusElement, Endpoint::getStatusElement));
	}

	@Override
	protected Element toElement(Endpoint resource)
	{
		List<ElementSystemValue> identifier = getIdentifiers(resource, Endpoint::hasIdentifier,
				Endpoint::getIdentifier);

		ElementSystemValue connectionType = resource.hasConnectionType()
				? ElementSystemValue.from(resource.getConnectionType())
				: null;

		String name = getString(resource, Endpoint::hasNameElement, Endpoint::getNameElement);

		ElementId managingOrganization = ElementId.from(resource, Endpoint::hasManagingOrganization,
				Endpoint::getManagingOrganization);

		List<ElementSystemValue> payloadType = resource.hasPayloadType()
				? resource.getPayloadType().stream().map(CodeableConcept::getCoding).flatMap(List::stream)
						.map(ElementSystemValue::from).toList()
				: null;

		List<String> payloadMimeType = resource.hasPayloadMimeType()
				? resource.getPayloadMimeType().stream().filter(CodeType::hasValue).map(CodeType::getValue).toList()
				: null;

		String address = getUrl(resource, Endpoint::hasAddressElement, Endpoint::getAddressElement);

		return new Element(nullIfEmpty(identifier), connectionType, name, managingOrganization,
				nullIfEmpty(payloadType), nullIfEmpty(payloadMimeType), address);
	}
}
