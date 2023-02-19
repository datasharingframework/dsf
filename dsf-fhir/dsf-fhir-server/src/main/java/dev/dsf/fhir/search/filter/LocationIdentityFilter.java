package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class LocationIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_locations";
	private static final String RESOURCE_ID_COLUMN = "location_id";

	public LocationIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public LocationIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
