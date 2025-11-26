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
package dev.dsf.bpe.v2.activity;

import java.util.function.Function;

import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ServiceTaskDelegate extends AbstractProcessPluginDelegate<ServiceTask> implements JavaDelegate
{
	public ServiceTaskDelegate(ProcessPluginApi api, Function<DelegateExecution, Variables> variablesFactory,
			ServiceTask delegate)
	{
		super(api, variablesFactory, delegate);
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception
	{
		Variables variables = createVariables(execution);

		try
		{
			delegate.execute(api, variables);
		}
		// do not stop process execution
		catch (ErrorBoundaryEvent event)
		{
			handleErrorBoundaryEvent(variables, event);
		}
		// stop process execution if exception not absorbed by error handler
		catch (Exception exception)
		{
			ServiceTaskErrorHandler handler = delegate.getErrorHandler();
			if (handler != null)
				exception = handler.handleException(api, variables, exception);

			// do not stop process execution if exception translated to error boundary event
			if (exception instanceof ErrorBoundaryEvent event)
				handleErrorBoundaryEvent(variables, event);

			else if (exception != null)
				execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
						exception.getMessage());
			// else, do nothing if exception was absorbed by error handler
		}
	}

	private void handleErrorBoundaryEvent(Variables variables, ErrorBoundaryEvent event)
	{
		ServiceTaskErrorHandler handler = delegate.getErrorHandler();
		if (handler != null)
			event = handler.handleErrorBoundaryEvent(api, variables, event);

		// do nothing if event was absorbed by error handler
		if (event != null)
			throw new BpmnError(event.getErrorCode(), event.getErrorMessage(), event);
	}
}
