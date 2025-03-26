package dev.dsf.bpe.test;

import java.util.function.Consumer;

import org.hl7.fhir.r4.model.StringType;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.variables.Variables;

public abstract class AbstractTest
{
	protected void executeTests(ProcessPluginApi api, Variables variables, Object... otherTestMethodArgs)
			throws Exception
	{
		PluginTestExecutor.execute(this, output(api, variables, "test-method-succeeded"),
				output(api, variables, "test-method-failed"), () -> variables.updateTask(variables.getStartTask()), api,
				variables, otherTestMethodArgs);
	}

	private Consumer<String> output(ProcessPluginApi api, Variables variables, String code)
	{
		return t -> variables.getStartTask().addOutput(
				api.getTaskHelper().createOutput(new StringType(t), "http://dsf.dev/fhir/CodeSystem/test", code));
	}
}
