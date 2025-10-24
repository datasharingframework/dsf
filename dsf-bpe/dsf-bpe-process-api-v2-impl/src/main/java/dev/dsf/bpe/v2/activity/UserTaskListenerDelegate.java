package dev.dsf.bpe.v2.activity;

import java.util.function.Function;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

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
					delegateTask.getBpmnModelElementInstance().getCamundaFormKey()));
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
