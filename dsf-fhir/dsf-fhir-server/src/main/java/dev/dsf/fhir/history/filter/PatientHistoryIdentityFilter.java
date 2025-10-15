package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;
import dev.dsf.fhir.search.filter.PatientIdentityFilter;

public class PatientHistoryIdentityFilter extends PatientIdentityFilter implements HistoryIdentityFilter
{
	private static final FhirServerRole HISTORY_ROLE = FhirServerRoleImpl.history(ResourceType.Patient);
	private static final String RESOURCE_TYPE = ResourceType.Patient.name();

	public PatientHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN, HISTORY_ROLE);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
