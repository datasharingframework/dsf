package dev.dsf.bpe.v2.activity;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ServiceTaskDelegate extends AbstractProcessPluginDelegate<ServiceTask> implements JavaDelegate
{
	public ServiceTaskDelegate(ProcessPluginApi api, ObjectMapper objectMapper, ServiceTask delegate)
	{
		super(api, objectMapper, delegate);
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception
	{
		Variables variables = creatVariables(execution);

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
