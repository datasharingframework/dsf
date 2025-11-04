/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
