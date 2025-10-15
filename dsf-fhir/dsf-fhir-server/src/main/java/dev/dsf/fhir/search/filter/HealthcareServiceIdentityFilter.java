package dev.dsf.fhir.search.filter;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;

public class HealthcareServiceIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final FhirServerRole SEARCH_ROLE = FhirServerRoleImpl.search(ResourceType.HealthcareService);
	private static final FhirServerRole READ_ROLE = FhirServerRoleImpl.read(ResourceType.HealthcareService);

	private static final String RESOURCE_TABLE = "current_healthcare_services";
	private static final String RESOURCE_ID_COLUMN = "healthcare_service_id";

	public HealthcareServiceIdentityFilter(Identity identity)
	{
		this(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN, SEARCH_ROLE);
	}

	public HealthcareServiceIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn,
			FhirServerRole operationRole)
	{
		super(identity, resourceTable, resourceIdColumn, operationRole, READ_ROLE);
	}
}
