package dev.dsf.bpe.v2.activity;

import org.camunda.bpm.engine.delegate.DelegateExecution;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ExecutionListenerErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ExecutionListenerDelegate extends AbstractProcessPluginDelegate<ExecutionListener>
		implements org.camunda.bpm.engine.delegate.ExecutionListener
{
	public ExecutionListenerDelegate(ProcessPluginApi api, ObjectMapper objectMapper, ExecutionListener delegate)
	{
		super(api, objectMapper, delegate);
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
