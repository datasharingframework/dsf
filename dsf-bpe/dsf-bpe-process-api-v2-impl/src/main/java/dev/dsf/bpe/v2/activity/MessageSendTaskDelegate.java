package dev.dsf.bpe.v2.activity;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.MessageSendTaskErrorHandler;
import dev.dsf.bpe.v2.variables.VariablesImpl;

public class MessageSendTaskDelegate extends AbstractMessageDelegate<MessageSendTask> implements JavaDelegate
{
	public MessageSendTaskDelegate(ProcessPluginApi api, MessageSendTask delegate, SendTaskValues sendTask)
	{
		super(api, delegate, sendTask);
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception
	{
		final VariablesImpl variables = new VariablesImpl(execution);

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
