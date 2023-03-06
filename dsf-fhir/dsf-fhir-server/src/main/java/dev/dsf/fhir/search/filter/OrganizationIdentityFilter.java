package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class OrganizationIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_organizations";
	private static final String RESOURCE_ID_COLUMN = "organization_id";

	public OrganizationIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public OrganizationIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
