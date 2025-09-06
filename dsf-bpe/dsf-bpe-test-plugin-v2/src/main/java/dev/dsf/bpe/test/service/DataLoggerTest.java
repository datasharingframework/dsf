package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import org.hl7.fhir.r4.model.Patient;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.service.DataLogger;
import dev.dsf.bpe.v2.variables.Variables;

public class DataLoggerTest extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, api.getDataLogger());
	}

	@PluginTest
	public void testIsEnabled(DataLogger dataLogger)
	{
		expectNotNull(dataLogger);
		expectTrue(dataLogger.isEnabled());
	}

	@PluginTest
	public void testLogFhirResource(DataLogger dataLogger)
	{
		expectNotNull(dataLogger);

		Patient patient = new Patient();
		patient.addName().setText("Test Patient");
		dataLogger.log("Test Patient", patient);
	}

	@PluginTest
	public void testLogObject(DataLogger dataLogger)
	{
		expectNotNull(dataLogger);

		dataLogger.log("Test Object", "Test String Object");
	}
}
