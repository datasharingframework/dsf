package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.ActivityDefinition;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.search.filter.ActivityDefinitionIdentityFilter;

public class ActivityDefinitionHistoryIdentityFilter extends ActivityDefinitionIdentityFilter
		implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = ActivityDefinition.class.getAnnotation(ResourceDef.class).name();

	public ActivityDefinitionHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
