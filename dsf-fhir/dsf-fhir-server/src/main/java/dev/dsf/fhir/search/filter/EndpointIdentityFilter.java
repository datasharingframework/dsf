package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;

public class EndpointIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_endpoints";
	private static final String RESOURCE_ID_COLUMN = "endpoint_id";

	public EndpointIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public EndpointIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
