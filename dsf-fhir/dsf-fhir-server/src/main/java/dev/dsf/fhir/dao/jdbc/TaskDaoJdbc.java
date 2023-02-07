package dev.dsf.fhir.dao.jdbc;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Task;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.TaskDao;
import dev.dsf.fhir.search.parameters.TaskAuthoredOn;
import dev.dsf.fhir.search.parameters.TaskIdentifier;
import dev.dsf.fhir.search.parameters.TaskModified;
import dev.dsf.fhir.search.parameters.TaskRequester;
import dev.dsf.fhir.search.parameters.TaskStatus;
import dev.dsf.fhir.search.parameters.user.TaskUserFilter;

public class TaskDaoJdbc extends AbstractResourceDaoJdbc<Task> implements TaskDao
{
	public TaskDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Task.class, "tasks", "task", "task_id",
				TaskUserFilter::new,
				with(TaskAuthoredOn::new, TaskIdentifier::new, TaskModified::new, TaskRequester::new, TaskStatus::new),
				with());
	}

	@Override
	protected Task copy(Task resource)
	{
		return resource.copy();
	}
}
