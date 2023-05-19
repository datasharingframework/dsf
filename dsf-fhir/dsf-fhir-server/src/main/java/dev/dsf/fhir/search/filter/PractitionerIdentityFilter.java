package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;

public class PractitionerIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_practitioners";
	private static final String RESOURCE_ID_COLUMN = "practitioner_id";

	public PractitionerIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public PractitionerIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
