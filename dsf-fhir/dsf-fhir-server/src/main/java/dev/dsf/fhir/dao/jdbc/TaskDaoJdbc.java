package dev.dsf.fhir.dao.jdbc;

import java.util.List;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Task;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.search.filter.TaskIdentityFilter;
import dev.dsf.fhir.search.parameters.TaskAuthoredOn;
import dev.dsf.fhir.search.parameters.TaskIdentifier;
import dev.dsf.fhir.search.parameters.TaskModified;
import dev.dsf.fhir.search.parameters.TaskRequester;
import dev.dsf.fhir.search.parameters.TaskStatus;

public class TaskDaoJdbc extends AbstractResourceDaoJdbc<Task> implements TaskDao
{
	public TaskDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Task.class, "tasks", "task", "task_id",
				TaskIdentityFilter::new,
				List.of(factory(TaskAuthoredOn.PARAMETER_NAME, TaskAuthoredOn::new),
						factory(TaskIdentifier.PARAMETER_NAME, TaskIdentifier::new, TaskIdentifier.getNameModifiers()),
						factory(TaskModified.PARAMETER_NAME, TaskModified::new),
						factory(TaskRequester.PARAMETER_NAME, TaskRequester::new, TaskRequester.getNameModifiers(),
								TaskRequester::new, TaskRequester.getIncludeParameterValues()),
						factory(TaskStatus.PARAMETER_NAME, TaskStatus::new, TaskStatus.getNameModifiers())),
				List.of());
	}

	@Override
	protected Task copy(Task resource)
	{
		return resource.copy();
	}
}
