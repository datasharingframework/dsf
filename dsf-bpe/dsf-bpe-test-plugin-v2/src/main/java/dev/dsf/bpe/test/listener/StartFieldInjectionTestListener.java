package dev.dsf.bpe.test.listener;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ExecutionListener;
import dev.dsf.bpe.v2.variables.Variables;

public class StartFieldInjectionTestListener implements ExecutionListener
{
	public static final int INT_VARIABLE_VALUE = 42;

	private String testVariable;

	public void setTestVariable(String testVariable)
	{
		this.testVariable = testVariable;
	}

	@Override
	public void notify(ProcessPluginApi api, Variables variables) throws Exception
	{
		variables.setString("testVariable", testVariable);
		variables.setInteger("intVariable", INT_VARIABLE_VALUE);
	}
}
