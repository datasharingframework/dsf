package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;
import dev.dsf.fhir.search.filter.MeasureIdentityFilter;

public class MeasureHistoryIdentityFilter extends MeasureIdentityFilter implements HistoryIdentityFilter
{
	private static final FhirServerRole HISTORY_ROLE = FhirServerRoleImpl.history(ResourceType.Measure);
	private static final String RESOURCE_TYPE = ResourceType.Measure.name();

	public MeasureHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN, HISTORY_ROLE);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
