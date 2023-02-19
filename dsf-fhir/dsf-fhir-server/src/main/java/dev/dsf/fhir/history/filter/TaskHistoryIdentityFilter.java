package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.Task;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.search.filter.TaskIdentityFilter;

public class TaskHistoryIdentityFilter extends TaskIdentityFilter implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = Task.class.getAnnotation(ResourceDef.class).name();

	public TaskHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
