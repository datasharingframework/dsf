package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;

public class NamingSystemIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_naming_systems";
	private static final String RESOURCE_ID_COLUMN = "naming_system_id";

	public NamingSystemIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public NamingSystemIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
