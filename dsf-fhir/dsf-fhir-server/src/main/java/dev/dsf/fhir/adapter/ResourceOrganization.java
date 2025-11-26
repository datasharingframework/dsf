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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;

public class ResourceOrganization extends AbstractResource<Organization>
{
	private record HtmlAddressElement(List<String> line, String postalCodeAndCity, String country)
	{
		static HtmlAddressElement from(Address address)
		{
			if (address == null)
				return null;

			List<String> line = address.hasLine()
					? address.getLine().stream().filter(StringType::hasValue).map(StringType::getValue).toList()
					: null;
			line = line != null && line.isEmpty() ? null : line;

			String postalCode = address.hasPostalCodeElement() && address.getPostalCodeElement().hasValue()
					? address.getPostalCodeElement().getValue()
					: null;
			String city = address.hasCityElement() && address.getCityElement().hasValue()
					? address.getCityElement().getValue()
					: null;
			String country = address.hasCountryElement() && address.getCountryElement().hasValue()
					? address.getCountryElement().getValue()
					: null;

			return line != null || postalCode != null || city != null || country != null ? new HtmlAddressElement(line,
					Stream.of(postalCode, city).filter(s -> s != null).collect(Collectors.joining(" ")), country)
					: null;
		}
	}

	private record Element(List<ElementSystemValue> identifier, List<ElementSystemValue> type, String name,
			List<String> alias, List<ElementSystemValue> telecom, List<HtmlAddressElement> address,
			List<ElementId> endpoint)
	{
	}

	public ResourceOrganization()
	{
		super(Organization.class,
				ActiveOrStatus.active(Organization::hasActiveElement, Organization::getActiveElement));
	}

	@Override
	protected Element toElement(Organization resource)
	{
		List<ElementSystemValue> identifier = getIdentifiers(resource, Organization::hasIdentifier,
				Organization::getIdentifier);

		List<ElementSystemValue> type = resource.hasType()
				? resource.getType().stream().map(CodeableConcept::getCoding).flatMap(List::stream)
						.map(ElementSystemValue::from).toList()
				: null;

		String name = getString(resource, Organization::hasNameElement, Organization::getNameElement);

		List<String> alias = resource.hasAlias()
				? resource.getAlias().stream().filter(StringType::hasValue).map(StringType::getValue).toList()
				: null;

		List<ElementSystemValue> telecom = resource.hasTelecom() ? resource.getTelecom().stream()
				.filter(ContactPoint::hasSystemElement).filter(ContactPoint::hasValueElement)
				.filter(p -> p.getSystemElement().hasValue()).filter(p -> p.getValueElement().hasValue()).map(p ->
				{
					String system = p.getSystemElement().getValue().getDisplay();
					String value = p.getValueElement().getValue();
					return ElementSystemValue.from(system, value);
				}).toList() : null;

		List<HtmlAddressElement> address = resource.hasAddress()
				? resource.getAddress().stream().map(HtmlAddressElement::from).filter(a -> a != null).toList()
				: null;

		List<ElementId> endpoint = ElementId.fromList(resource, Organization::hasEndpoint, Organization::getEndpoint);

		return new Element(nullIfEmpty(identifier), nullIfEmpty(type), name, nullIfEmpty(alias), nullIfEmpty(telecom),
				nullIfEmpty(address), endpoint);
	}
}
