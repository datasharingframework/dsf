package dev.dsf.bpe.test.service;

import java.util.function.Consumer;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.hl7.fhir.r4.model.StringType;

import dev.dsf.bpe.test.PluginTestExecutor;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.activity.AbstractServiceDelegate;
import dev.dsf.bpe.v1.variables.Variables;

public abstract class AbstractTest extends AbstractServiceDelegate
{
	public AbstractTest(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		PluginTestExecutor.execute(this, output(variables, "test-method-succeeded"),
				output(variables, "test-method-failed"), () -> variables.updateTask(variables.getStartTask()));
	}

	private Consumer<String> output(Variables variables, String code)
	{
		return t -> variables.getStartTask().addOutput(
				api.getTaskHelper().createOutput(new StringType(t), "http://dsf.dev/fhir/CodeSystem/test", code));
	}
}
