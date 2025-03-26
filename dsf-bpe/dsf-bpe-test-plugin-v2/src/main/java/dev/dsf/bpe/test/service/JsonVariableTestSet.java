package dev.dsf.bpe.test.service;

import dev.dsf.bpe.test.json.JsonPojo;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class JsonVariableTestSet implements ServiceTask
{
	public static final String JSON_VARIABLE = "json-variable";
	public static final String STRING_VARIABLE = "string-variable";
	public static final String INTEGER_VARIABLE = "integer-variable";

	public static final String TEST_VALUE_1 = "test-value-1";
	public static final String TEST_VALUE_2 = "test-value-2";
	public static final String TEST_STRING = "test-string";
	public static final Integer TEST_INTEGER = 42;

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		variables.setJsonVariable(JSON_VARIABLE, new JsonPojo(TEST_VALUE_1, TEST_VALUE_2));
		variables.setString(STRING_VARIABLE, TEST_STRING);
		variables.setInteger(INTEGER_VARIABLE, TEST_INTEGER);
	}
}
