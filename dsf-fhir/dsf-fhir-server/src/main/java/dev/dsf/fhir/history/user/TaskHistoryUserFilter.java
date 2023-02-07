package dev.dsf.fhir.history.user;

import org.hl7.fhir.r4.model.Task;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.search.parameters.user.TaskUserFilter;

public class TaskHistoryUserFilter extends TaskUserFilter implements HistoryUserFilter
{
	private static final String RESOURCE_TYPE = Task.class.getAnnotation(ResourceDef.class).name();

	public TaskHistoryUserFilter(User user)
	{
		super(user, HistoryUserFilter.RESOURCE_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryUserFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
