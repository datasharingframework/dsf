package dev.dsf.fhir.history.user;

import org.hl7.fhir.r4.model.OrganizationAffiliation;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.search.parameters.user.OrganizationUserFilter;

public class OrganizationAffiliationHistoryUserFilter extends OrganizationUserFilter implements HistoryUserFilter
{
	private static final String RESOURCE_TYPE = OrganizationAffiliation.class.getAnnotation(ResourceDef.class).name();

	public OrganizationAffiliationHistoryUserFilter(User user)
	{
		super(user, HistoryUserFilter.RESOURCE_TABLE, HistoryUserFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryUserFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
