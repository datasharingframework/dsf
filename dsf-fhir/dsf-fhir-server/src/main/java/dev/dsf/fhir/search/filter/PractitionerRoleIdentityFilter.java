package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class PractitionerRoleIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_practitioner_roles";
	private static String RESOURCE_ID_COLUMN = "practitioner_role_id";

	public PractitionerRoleIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public PractitionerRoleIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
