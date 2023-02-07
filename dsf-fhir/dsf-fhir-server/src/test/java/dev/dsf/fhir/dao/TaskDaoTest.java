package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;

import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskStatus;

import dev.dsf.fhir.dao.jdbc.TaskDaoJdbc;

public class TaskDaoTest extends AbstractResourceDaoTest<Task, TaskDao>
{
	private static final TaskStatus status = TaskStatus.REQUESTED;
	private static final String description = "Demo Task Description";

	public TaskDaoTest()
	{
		super(Task.class, TaskDaoJdbc::new);
	}

	@Override
	public Task createResource()
	{
		Task task = new Task();
		task.setStatus(status);
		return task;
	}

	@Override
	protected void checkCreated(Task resource)
	{
		assertEquals(status, resource.getStatus());
	}

	@Override
	protected Task updateResource(Task resource)
	{
		resource.setDescription(description);
		return resource;
	}

	@Override
	protected void checkUpdates(Task resource)
	{
		assertEquals(description, resource.getDescription());
	}
}
