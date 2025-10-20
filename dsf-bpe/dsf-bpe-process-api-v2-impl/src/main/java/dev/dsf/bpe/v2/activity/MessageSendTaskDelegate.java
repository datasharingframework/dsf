package dev.dsf.bpe.v2.activity;

import java.util.function.Function;

import org.operaton.bpm.engine.delegate.BpmnError;
import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.JavaDelegate;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.MessageSendTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class MessageSendTaskDelegate extends AbstractMessageDelegate<MessageSendTask> implements JavaDelegate
{
	public MessageSendTaskDelegate(ProcessPluginApi api, Function<DelegateExecution, Variables> variablesFactory,
			MessageSendTask delegate, SendTaskValues sendTask)
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
		// do not stop process execution
		catch (ErrorBoundaryEvent event)
		{
			MessageSendTaskErrorHandler handler = delegate.getErrorHandler();
			if (handler != null)
				event = handler.handleErrorBoundaryEvent(api, variables, event);

			if (event != null)
				throw new BpmnError(event.getErrorCode(), event.getErrorMessage(), event);
			// else, do nothing if event was absorbed by error handler
		}
		// stop process execution if exception not absorbed by error handler
		catch (Exception exception)
		{
			MessageSendTaskErrorHandler handler = delegate.getErrorHandler();
			if (handler != null)
				exception = handler.handleException(api, variables, sendTaskValues, exception);

			if (exception != null)
				execution.getProcessEngine().getRuntimeService().deleteProcessInstance(execution.getProcessInstanceId(),
						exception.getMessage());
			// else, do nothing if exception was absorbed by error handler
		}
	}
}
