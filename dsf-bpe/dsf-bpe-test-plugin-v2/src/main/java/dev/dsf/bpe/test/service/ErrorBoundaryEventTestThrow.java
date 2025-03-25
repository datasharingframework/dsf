package dev.dsf.bpe.test.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.error.impl.ExceptionToErrorBoundaryEventTranslationErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ErrorBoundaryEventTestThrow implements ServiceTask
{
	public static final String TEST_ERROR_CODE = "testErrorCode";
	public static final String TEST_ERROR_MESSAGE = "testErrorMessage";

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		throw new RuntimeException(TEST_ERROR_MESSAGE);
	}

	@Override
	public ServiceTaskErrorHandler getErrorHandler()
	{
		return new ExceptionToErrorBoundaryEventTranslationErrorHandler(e -> TEST_ERROR_CODE);
	}
}
