package dev.dsf.bpe.test.service;

import java.nio.charset.StandardCharsets;

import org.hl7.fhir.r4.model.Binary;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class FhirBinaryVariableTestSet implements ServiceTask
{
	public static final String BINARY_VARIABLE = "binary-variable";
	public static final String STRING_VARIABLE = "string-variable";
	public static final String INTEGER_VARIABLE = "integer-variable";

	public static final byte[] TEST_DATA = "Hello World".getBytes(StandardCharsets.UTF_8);
	public static final String TEST_STRING = "test-string";
	public static final Integer TEST_INTEGER = 42;

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		Binary binary = new Binary();
		binary.setData(TEST_DATA);

		variables.setFhirResource(BINARY_VARIABLE, binary);
		variables.setString(STRING_VARIABLE, TEST_STRING);
		variables.setInteger(INTEGER_VARIABLE, TEST_INTEGER);
	}
}
