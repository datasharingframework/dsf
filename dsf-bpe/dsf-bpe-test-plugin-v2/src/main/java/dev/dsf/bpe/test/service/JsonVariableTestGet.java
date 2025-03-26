package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.test.json.JsonPojo;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class JsonVariableTestGet extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void getJsonVariable(Variables variables) throws Exception
	{
		JsonPojo variable = variables.getVariable(JsonVariableTestSet.JSON_VARIABLE);

		expectNotNull(variable);
		expectSame(JsonVariableTestSet.TEST_VALUE_1, variable.getValue1());
		expectSame(JsonVariableTestSet.TEST_VALUE_2, variable.getValue2());
	}

	@PluginTest
	public void getStringVariable(Variables variables) throws Exception
	{
		String variable = variables.getString(JsonVariableTestSet.STRING_VARIABLE);

		expectNotNull(variable);
		expectSame(JsonVariableTestSet.TEST_STRING, variable);
	}

	@PluginTest
	public void getIntegerVariable(Variables variables) throws Exception
	{
		Integer variable = variables.getVariable(JsonVariableTestSet.INTEGER_VARIABLE);

		expectNotNull(variable);
		expectSame(JsonVariableTestSet.TEST_INTEGER, variable);
	}
}
