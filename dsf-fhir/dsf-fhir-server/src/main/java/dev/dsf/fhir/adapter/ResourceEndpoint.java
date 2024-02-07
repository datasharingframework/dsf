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
