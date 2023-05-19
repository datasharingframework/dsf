package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;

public class OrganizationAffiliationIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_organization_affiliations";
	private static final String RESOURCE_ID_COLUMN = "organization_affiliation_id";

	public OrganizationAffiliationIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public OrganizationAffiliationIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
