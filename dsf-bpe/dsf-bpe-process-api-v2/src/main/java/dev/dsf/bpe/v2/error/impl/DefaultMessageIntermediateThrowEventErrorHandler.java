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

import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.constants.CodeSystems.BpmnMessage;
import dev.dsf.bpe.v2.error.MessageIntermediateThrowEventErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class DefaultMessageIntermediateThrowEventErrorHandler extends AbstractMessageActivityErrorHandler
		implements MessageIntermediateThrowEventErrorHandler
{
	private static final Logger logger = LoggerFactory
			.getLogger(DefaultMessageIntermediateThrowEventErrorHandler.class);

	/**
	 * Logs the given {@link Exception}, updates all received {@link Task} resource with {@link TaskStatus#INPROGRESS}
	 * to {@link TaskStatus#FAILED} and adds a {@link TaskOutputComponent} of type {@link BpmnMessage#error()}.
	 * <p>
	 * Exceptions thrown while updating the {@link Task} resources to status failed are logged.
	 * <p>
	 * Returns the given {@link Exception} resulting in a deleted i.e. stopped process instance.
	 */
	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception)
	{
		logger.debug("Error while executing Task message send {}", getClass().getName(), exception);
		logger.error("Process {} has fatal error in step {} for task {}, reason: {} - {}",
				variables.getProcessDefinitionId(), variables.getActivityInstanceId(),
				api.getTaskHelper().getLocalVersionlessAbsoluteUrl(variables.getStartTask()),
				exception.getClass().getName(), exception.getMessage());

		String errorMessage = createErrorMessage(api, variables, exception, sendTaskValues, this::getExceptionMessage);

		List<Task> tasks = getTasks(api, variables, exception);
		updateFailedIfInprogress(api, variables, errorMessage, tasks);

		return exception;
	}
}
