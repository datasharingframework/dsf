package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.Endpoint;

public class SearchSetEndpoint extends AbstractSearchSet<Endpoint>
{
	private record Row(ElementId id, String status, String identifier, String name, String address,
			ElementId managingOrganization, String lastUpdated)
	{
	}

	public SearchSetEndpoint(int defaultPageCount)
	{
		super(defaultPageCount, Endpoint.class);
	}

	@Override
	protected Row toRow(ElementId id, Endpoint resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String identifier = getIdentifierValues(resource, Endpoint::hasIdentifier, Endpoint::getIdentifier,
				NAMING_SYSTEM_ENDPOINT_IDENTIFIER);
		String name = resource.hasName() ? resource.getName() : "";
		String address = resource.hasAddress() ? resource.getAddress() : "";

		ElementId managingOrganization = ElementId.from(resource, Endpoint::hasManagingOrganization,
				Endpoint::getManagingOrganization);

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, identifier, name, address, managingOrganization, lastUpdated);
	}
}
