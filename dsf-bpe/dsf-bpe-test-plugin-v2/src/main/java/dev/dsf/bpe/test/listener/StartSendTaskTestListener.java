package dev.dsf.bpe.test.listener;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ExecutionListener;
import dev.dsf.bpe.v2.variables.Variables;

public class StartSendTaskTestListener implements ExecutionListener
{
	public static final String TEST_VARIABLE_VALUE = "testVariableValue";

	@Override
	public void notify(ProcessPluginApi api, Variables variables) throws Exception
	{
		variables.setString("testVariable", TEST_VARIABLE_VALUE);
	}
}
