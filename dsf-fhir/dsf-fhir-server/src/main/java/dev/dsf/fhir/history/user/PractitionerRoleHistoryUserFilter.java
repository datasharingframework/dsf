package dev.dsf.fhir.history.user;

import org.hl7.fhir.r4.model.PractitionerRole;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.search.parameters.user.PractitionerRoleUserFilter;

public class PractitionerRoleHistoryUserFilter extends PractitionerRoleUserFilter implements HistoryUserFilter
{
	private static final String RESOURCE_TYPE = PractitionerRole.class.getAnnotation(ResourceDef.class).name();

	public PractitionerRoleHistoryUserFilter(User user)
	{
		super(user, HistoryUserFilter.RESOURCE_TABLE, HistoryUserFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryUserFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
