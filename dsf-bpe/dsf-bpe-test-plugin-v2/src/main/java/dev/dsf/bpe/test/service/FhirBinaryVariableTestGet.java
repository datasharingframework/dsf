package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import org.hl7.fhir.r4.model.Binary;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class FhirBinaryVariableTestGet extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void getBinaryVariable(Variables variables) throws Exception
	{
		Binary binary = variables.getFhirResource(FhirBinaryVariableTestSet.BINARY_VARIABLE);

		expectNotNull(binary);
		expectSame(FhirBinaryVariableTestSet.TEST_DATA, binary.getData());
	}

	@PluginTest
	public void getStringVariable(Variables variables) throws Exception
	{
		String variable = variables.getString(FhirBinaryVariableTestSet.STRING_VARIABLE);

		expectNotNull(variable);
		expectSame(FhirBinaryVariableTestSet.TEST_STRING, variable);
	}

	@PluginTest
	public void getIntegerVariable(Variables variables) throws Exception
	{
		Integer variable = variables.getVariable(FhirBinaryVariableTestSet.INTEGER_VARIABLE);

		expectNotNull(variable);
		expectSame(FhirBinaryVariableTestSet.TEST_INTEGER, variable);
	}
}
