package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class HealthcareServiceIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_healthcare_services";
	private static final String RESOURCE_ID_COLUMN = "healthcare_service_id";

	public HealthcareServiceIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public HealthcareServiceIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
