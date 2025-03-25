package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.test.listener.StartFieldInjectionTestListener;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class FieldInjectionTest extends AbstractTest implements ServiceTask
{
	private String stringField;
	private String stringFieldFromExpression;
	private int intField;

	public void setStringField(String stringField)
	{
		this.stringField = stringField;
	}

	public void setStringFieldFromExpression(String stringFieldFromExpression)
	{
		this.stringFieldFromExpression = stringFieldFromExpression;
	}

	public void setIntField(int intField)
	{
		this.intField = intField;
	}

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void testStringField(ProcessPluginApi api) throws Exception
	{
		expectSame("stringFieldValue", stringField);
	}

	@PluginTest
	public void testStringFieldFromExpression(ProcessPluginApi api) throws Exception
	{
		expectSame("testVariableValue", stringFieldFromExpression);
	}

	@PluginTest
	public void testIntgField(ProcessPluginApi api) throws Exception
	{
		expectSame(StartFieldInjectionTestListener.INT_VARIABLE_VALUE, intField);
	}
}
