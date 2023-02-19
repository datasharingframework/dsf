package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class PatientIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_patients";
	private static final String RESOURCE_ID_COLUMN = "patient_id";

	public PatientIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public PatientIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
