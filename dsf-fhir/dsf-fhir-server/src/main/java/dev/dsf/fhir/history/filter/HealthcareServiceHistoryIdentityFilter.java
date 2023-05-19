package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.HealthcareService;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.search.filter.HealthcareServiceIdentityFilter;

public class HealthcareServiceHistoryIdentityFilter extends HealthcareServiceIdentityFilter
		implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = HealthcareService.class.getAnnotation(ResourceDef.class).name();

	public HealthcareServiceHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
