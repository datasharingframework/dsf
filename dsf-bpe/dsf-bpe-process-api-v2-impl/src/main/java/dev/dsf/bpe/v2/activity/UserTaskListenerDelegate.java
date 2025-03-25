package dev.dsf.bpe.v2.activity;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.CreateQuestionnaireResponseValues;
import dev.dsf.bpe.v2.error.UserTaskListenerErrorHandler;
import dev.dsf.bpe.v2.variables.VariablesImpl;

public class UserTaskListenerDelegate implements TaskListener
{
	private final ProcessPluginApi api;
	private final UserTaskListener delegate;

	public UserTaskListenerDelegate(ProcessPluginApi api, UserTaskListener delegate)
	{
		this.api = api;
		this.delegate = delegate;
	}

	@Override
	public void notify(DelegateTask delegateTask)
	{
		final VariablesImpl variables = new VariablesImpl(delegateTask.getExecution());

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
				DelegateExecution execution = delegateTask.getExecution();
				delegateTask.getExecution().getProcessEngine().getRuntimeService()
						.deleteProcessInstance(execution.getProcessInstanceId(), exception.getMessage());
			}
			// else, do nothing if exception was absorbed by error handler
		}
	}
}
