package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.PractitionerRole;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.search.filter.PractitionerRoleIdentityFilter;

public class PractitionerRoleHistoryIdentityFilter extends PractitionerRoleIdentityFilter
		implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = PractitionerRole.class.getAnnotation(ResourceDef.class).name();

	public PractitionerRoleHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
