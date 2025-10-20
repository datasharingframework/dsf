package dev.dsf.bpe.v2.activity;

import java.util.function.Function;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.error.MessageIntermediateThrowEventErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class MessageIntermediateThrowEventDelegate extends AbstractMessageDelegate<MessageIntermediateThrowEvent>
		implements JavaDelegate
{
	public MessageIntermediateThrowEventDelegate(ProcessPluginApi api,
			Function<DelegateExecution, Variables> variablesFactory, MessageIntermediateThrowEvent delegate,
			SendTaskValues sendTask)
	{
		super(api, variablesFactory, delegate, sendTask);
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception
	{
		Variables variables = createVariables(execution);

		try
		{
			delegate.execute(api, variables, sendTaskValues);
		}
		catch (Exception exception)
		{
			MessageIntermediateThrowEventErrorHandler handler = delegate.getErrorHandler();
			if (handler != null)
				exception = handler.handleException(api, variables, sendTaskValues, exception);

			if (exception != null)
				execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
						exception.getMessage());
			// else, do nothing if exception was absorbed by error handler
		}
	}
}
