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
package dev.dsf.bpe.v2.error.impl;

import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ErrorBoundaryEventErrorHandler;
import dev.dsf.bpe.v2.error.ExceptionErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public abstract class AbstractErrorHandler implements ErrorBoundaryEventErrorHandler, ExceptionErrorHandler
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractErrorHandler.class);

	/**
	 * Logs the given {@link ErrorBoundaryEvent} and returns the event without modification.
	 */
	@Override
	public ErrorBoundaryEvent handleErrorBoundaryEvent(ProcessPluginApi api, Variables variables,
			ErrorBoundaryEvent event)
	{
		logger.debug("Error while executing service task {}", variables.getActivityInstanceId(), event);
		logger.warn(
				"Process {} encountered error boundary event in step {} for service task {}, error-code: {}, error-message: {}",
				variables.getProcessDefinitionId(), variables.getActivityInstanceId(),
				api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()), event.getErrorCode(),
				event.getErrorMessage());

		return event;
	}

	/**
	 * Logs the given {@link Exception}, updates all received {@link Task} resource with {@link TaskStatus#INPROGRESS}
	 * to {@link TaskStatus#FAILED} and adds a {@link TaskOutputComponent} of type {@link BpmnMessage#error()}.
	 * <p>
	 * Exceptions thrown while updating the {@link Task} resources to status failed are logged.
	 * <p>
	 * Returns the given {@link Exception} resulting in a deleted i.e. stopped process instance.
	 */
	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, Exception exception)
	{
		logger.debug("Error while executing service task {}", variables.getActivityInstanceId(), exception);
		logger.warn("Process {} has fatal error in step {} for service task {}, reason: {} - {}",
				variables.getProcessDefinitionId(), variables.getActivityInstanceId(),
				api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
				exception.getClass().getName(), exception.getMessage());

		String errorMessage = createErrorMessageFromException(api, variables, exception);
		List<Task> tasks = getTasks(api, variables, exception);

		updateFailedIfInprogress(api, variables, errorMessage, tasks);

		return exception;
	}

	// TODO javadoc, how / when to override
	protected List<Task> getTasks(ProcessPluginApi api, Variables variables, Exception exception)
	{
		return variables.getTasks();
	}

	// TODO javadoc, how / when to override
	protected void updateFailedIfInprogress(ProcessPluginApi api, Variables variables, String errorMessage,
			List<Task> tasks)
	{
		for (int i = tasks.size() - 1; i >= 0; i--)
		{
			Task task = tasks.get(i);

			if (TaskStatus.INPROGRESS.equals(task.getStatus()))
			{
				task.setStatus(Task.TaskStatus.FAILED);

				TaskOutputComponent output = createTaskErrorOutput(api, variables, errorMessage);
				if (output != null)
					task.addOutput(output);

				updateTaskAndHandleException(api, variables, task);
			}
			else
			{
				logger.debug("Not updating Task {} with status: {}",
						api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task), task.getStatus());
			}
		}
	}

	// TODO javadoc, how / when to override
	protected TaskOutputComponent createTaskErrorOutput(ProcessPluginApi api, Variables variables, String errorMessage)
	{
		return new TaskOutputComponent(new CodeableConcept(BpmnMessage.error()), new StringType(errorMessage));
	}

	// TODO javadoc, how / when to override
	protected String createErrorMessageFromException(ProcessPluginApi api, Variables variables, Exception exception)
	{
		return "Process " + variables.getProcessDefinitionId() + " has fatal error in step "
				+ variables.getActivityInstanceId() + ", reason: " + exception.getMessage();
	}

	// TODO javadoc, how / when to override
	protected void updateTaskAndHandleException(ProcessPluginApi api, Variables variables, Task task)
	{
		try
		{
			logger.debug("Updating Task {}, new status: {}", api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task),
					task.getStatus().toCode());

			api.getDsfClientProvider().getLocalDsfClient().withMinimalReturn().update(task);
		}
		catch (Exception e)
		{
			logger.debug("Unable to update Task {}", api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task), e);
			logger.error("Unable to update Task {}: {} - {}", api.getTaskHelper().getLocalVersionlessAbsoluteUrl(task),
					e.getClass().getName(), e.getMessage());
		}
	}
}
