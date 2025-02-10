package dev.dsf.bpe.test.service;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public class TestActivitySelector extends AbstractServiceDelegate
{
	public TestActivitySelector(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		String testActivity = api.getTaskHelper().getFirstInputParameterStringValue(variables.getStartTask(),
				"http://dsf.dev/fhir/CodeSystem/test", "test-activity").get();
		variables.setString("testActivity", testActivity);
	}
}
