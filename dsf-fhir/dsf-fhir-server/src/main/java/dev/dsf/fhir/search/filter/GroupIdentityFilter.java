package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class GroupIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_groups";
	private static final String RESOURCE_ID_COLUMN = "group_id";

	public GroupIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public GroupIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
