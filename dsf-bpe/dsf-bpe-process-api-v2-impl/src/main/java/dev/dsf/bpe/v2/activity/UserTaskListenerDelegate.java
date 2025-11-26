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
import org.operaton.bpm.engine.delegate.DelegateTask;
import org.operaton.bpm.engine.delegate.TaskListener;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.CreateQuestionnaireResponseValues;
import dev.dsf.bpe.v2.error.UserTaskListenerErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class UserTaskListenerDelegate extends AbstractProcessPluginDelegate<UserTaskListener> implements TaskListener
{
	public UserTaskListenerDelegate(ProcessPluginApi api, Function<DelegateExecution, Variables> variablesFactory,
			UserTaskListener delegate)
	{
		super(api, variablesFactory, delegate);
	}

	@Override
	public void notify(DelegateTask delegateTask)
	{
		DelegateExecution execution = delegateTask.getExecution();
		Variables variables = createVariables(execution);

		try
		{
			delegate.notify(api, variables, new CreateQuestionnaireResponseValues(delegateTask.getId(),
					delegateTask.getBpmnModelElementInstance().getOperatonFormKey()));
		}
		// stop process execution if exception not absorbed by error handler
		catch (Exception exception)
		{
			UserTaskListenerErrorHandler handler = delegate.getErrorHandler();
			if (handler != null)
				exception = handler.handleException(api, variables, exception);

			if (exception != null)
			{
				execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
						exception.getMessage());
			}
			// else, do nothing if exception was absorbed by error handler
		}
	}
}
