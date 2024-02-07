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
				? Boolean.TRUE.equals(resource.getActiveElement().getValue())
				: false;

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
