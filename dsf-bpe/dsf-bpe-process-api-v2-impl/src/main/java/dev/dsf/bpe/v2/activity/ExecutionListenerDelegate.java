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

import org.operaton.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ExecutionListenerErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ExecutionListenerDelegate extends AbstractProcessPluginDelegate<ExecutionListener>
		implements org.operaton.bpm.engine.delegate.ExecutionListener
{
	public ExecutionListenerDelegate(ProcessPluginApi api, Function<DelegateExecution, Variables> variablesFactory,
			ExecutionListener delegate)
	{
		super(api, variablesFactory, delegate);
	}

	@Override
	public void notify(DelegateExecution execution) throws Exception
	{
		Variables variables = createVariables(execution);

		try
		{
			delegate.notify(api, variables);
		}
		catch (Exception exception)
		{
			ExecutionListenerErrorHandler handler = delegate.getErrorHandler();
			if (handler != null)
				exception = handler.handleException(api, variables, exception);

			if (exception != null)
				execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
						exception.getMessage());
			// else, do nothing if exception was absorbed by error handler
		}
	}
}
