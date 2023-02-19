package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class BundleIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_bundles";
	private static final String RESOURCE_ID_COLUMN = "bundle_id";

	public BundleIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public BundleIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
