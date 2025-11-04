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
