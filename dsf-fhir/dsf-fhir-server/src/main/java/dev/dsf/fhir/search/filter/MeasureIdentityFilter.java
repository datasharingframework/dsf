package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class MeasureIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_measures";
	private static final String RESOURCE_ID_COLUMN = "measure_id";

	public MeasureIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public MeasureIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
