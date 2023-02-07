package dev.dsf.fhir.history.user;

import org.hl7.fhir.r4.model.HealthcareService;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.search.parameters.user.HealthcareServiceUserFilter;

public class HealthcareServiceHistoryUserFilter extends HealthcareServiceUserFilter implements HistoryUserFilter
{
	private static final String RESOURCE_TYPE = HealthcareService.class.getAnnotation(ResourceDef.class).name();

	public HealthcareServiceHistoryUserFilter(User user)
	{
		super(user, HistoryUserFilter.RESOURCE_TABLE, HistoryUserFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryUserFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
