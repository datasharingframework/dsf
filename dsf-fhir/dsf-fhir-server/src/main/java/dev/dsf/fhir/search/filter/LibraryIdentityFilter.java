package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class LibraryIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_libraries";
	private static final String RESOURCE_ID_COLUMN = "library_id";

	public LibraryIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public LibraryIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
