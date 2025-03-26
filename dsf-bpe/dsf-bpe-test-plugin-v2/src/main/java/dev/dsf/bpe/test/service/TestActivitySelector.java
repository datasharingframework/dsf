package dev.dsf.bpe.test.service;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class TestActivitySelector implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		String testActivity = api.getTaskHelper().getFirstInputParameterStringValue(variables.getStartTask(),
				"http://dsf.dev/fhir/CodeSystem/test", "test-activity").get();
		variables.setString("testActivity", testActivity);
	}
}
