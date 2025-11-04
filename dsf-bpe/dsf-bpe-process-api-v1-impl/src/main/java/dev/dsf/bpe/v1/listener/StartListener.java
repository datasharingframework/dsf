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
package dev.dsf.bpe.v1.listener;

import java.util.function.Function;

import org.hl7.fhir.r4.model.Task;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.api.Constants;
import dev.dsf.bpe.v1.constants.CodeSystems.BpmnMessage;

public class StartListener extends AbstractListener implements ExecutionListener
{
	private static final Logger logger = LoggerFactory.getLogger(StartListener.class);

	public StartListener(String serverBaseUrl, Function<DelegateExecution, ListenerVariables> variablesFactory)
	{
		super(serverBaseUrl, variablesFactory);
	}

	@Override
	public void doNotify(DelegateExecution execution, ListenerVariables variables) throws Exception
	{
		Task task = variables.getResource(Constants.TASK_VARIABLE);
		execution.removeVariable(Constants.TASK_VARIABLE);

		if (task != null)
		{
			variables.onStart(task);
			logStart(logger, task);
		}
		else
			logger.warn("Variable 'task' null, not updating tasks");
	}

	private void logStart(Logger logger, Task task)
	{
		String processUrl = task.getInstantiatesCanonical();
		String messageName = getFirstInputParameter(task, BpmnMessage.messageName());
		String businessKey = getFirstInputParameter(task, BpmnMessage.businessKey());
		String correlationKey = getFirstInputParameter(task, BpmnMessage.correlationKey());
		String taskUrl = getLocalVersionlessAbsoluteUrl(task);
		String requester = getRequesterIdentifierValue(task);

		if (correlationKey != null)
			logger.info(
					"Starting process {} at {} [task: {}, requester: {}, business-key: {}, correlation-key: {}, message: {}]",
					processUrl, getCurrentTime(), taskUrl, requester, businessKey, correlationKey, messageName);
		else
			logger.info("Starting process {} at {} [task: {}, requester: {}, business-key: {}, message: {}]",
					processUrl, getCurrentTime(), taskUrl, requester, businessKey, messageName);
	}
}
