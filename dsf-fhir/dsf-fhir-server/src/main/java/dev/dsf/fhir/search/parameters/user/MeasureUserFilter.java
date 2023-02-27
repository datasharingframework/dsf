package dev.dsf.fhir.search.parameters.user;

import dev.dsf.fhir.authentication.User;

public class MeasureUserFilter extends AbstractMetaTagAuthorizationRoleUserFilter
{
	private static final String RESOURCE_TABLE = "current_measures";
	private static final String RESOURCE_ID_COLUMN = "measure_id";

	public MeasureUserFilter(User user)
	{
		super(user, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public MeasureUserFilter(User user, String resourceTable, String resourceIdColumn)
	{
		super(user, resourceTable, resourceIdColumn);
	}
}
