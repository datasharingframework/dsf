package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class ValueSetIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_value_sets";
	private static final String RESOURCE_ID_COLUMN = "value_set_id";

	public ValueSetIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public ValueSetIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
