package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class SubscriptionIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_subscriptions";
	private static final String RESOURCE_ID_COLUMN = "subscription_id";

	public SubscriptionIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public SubscriptionIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
