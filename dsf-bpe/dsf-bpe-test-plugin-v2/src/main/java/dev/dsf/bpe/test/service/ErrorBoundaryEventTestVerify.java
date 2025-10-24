package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class ErrorBoundaryEventTestVerify extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void verifyErrorCode(Variables variables) throws Exception
	{
		expectSame(ErrorBoundaryEventTestThrow.TEST_ERROR_CODE, variables.getString("errorCodeVariable"));
	}

	@PluginTest
	public void verifyErrorMessage(Variables variables) throws Exception
	{
		expectSame(ErrorBoundaryEventTestThrow.TEST_ERROR_MESSAGE, variables.getString("errorMessageVariable"));
	}
}
