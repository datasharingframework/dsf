package dev.dsf.bpe.test.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultServiceTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ExceptionTest implements ServiceTask
{
	private static final Logger logger = LoggerFactory.getLogger(ExceptionTest.class);

	private static final String EXCEPTION_TEST_MESSAGE = "TestMessage";

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		throw new RuntimeException(EXCEPTION_TEST_MESSAGE);
	}

	@Override
	public ServiceTaskErrorHandler getErrorHandler()
	{
		return new DefaultServiceTaskErrorHandler()
		{
			@Override
			public Exception handleException(ProcessPluginApi api, Variables variables, Exception exception)
			{
				if (EXCEPTION_TEST_MESSAGE.equals(exception.getMessage()))
				{
					logger.info("Handling expected exception", exception);
					return null;
				}
				else
				{
					logger.warn("Handling unexpected exception", exception);
					return exception;
				}
			}
		};
	}
}
