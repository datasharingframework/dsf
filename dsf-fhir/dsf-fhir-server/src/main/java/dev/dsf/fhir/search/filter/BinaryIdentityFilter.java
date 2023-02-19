package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class BinaryIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_binaries";
	private static final String RESOURCE_ID_COLUMN = "binary_id";

	public BinaryIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public BinaryIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
