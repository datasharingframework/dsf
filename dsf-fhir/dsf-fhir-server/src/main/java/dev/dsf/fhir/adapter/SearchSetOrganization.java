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
