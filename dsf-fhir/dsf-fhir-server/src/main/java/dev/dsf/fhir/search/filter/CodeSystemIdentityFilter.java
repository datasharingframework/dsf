package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;

public class CodeSystemIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_code_systems";
	private static final String RESOURCE_ID_COLUMN = "code_system_id";

	public CodeSystemIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public CodeSystemIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
