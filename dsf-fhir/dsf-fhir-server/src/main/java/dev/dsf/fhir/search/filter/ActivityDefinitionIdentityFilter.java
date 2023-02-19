package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class ActivityDefinitionIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_activity_definitions";
	private static final String RESOURCE_ID_COLUMN = "activity_definition_id";

	public ActivityDefinitionIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public ActivityDefinitionIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
