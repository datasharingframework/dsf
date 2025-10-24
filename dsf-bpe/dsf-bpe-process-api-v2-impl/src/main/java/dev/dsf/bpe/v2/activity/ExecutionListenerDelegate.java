package dev.dsf.bpe.v2.activity;

import java.util.function.Function;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ExecutionListenerErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ExecutionListenerDelegate extends AbstractProcessPluginDelegate<ExecutionListener>
		implements org.camunda.bpm.engine.delegate.ExecutionListener
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
