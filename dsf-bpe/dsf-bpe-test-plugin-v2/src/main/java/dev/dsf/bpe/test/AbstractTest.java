package dev.dsf.bpe.test;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hl7.fhir.r4.model.StringType;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public abstract class AbstractTest
{
	public static final Function<Exception, Exception> TO_ERROR_BOUNDARY_EVENT = _ -> new ErrorBoundaryEvent(
			"test_failed", "test_failed");

	protected void executeTests(ProcessPluginApi api, Variables variables, Object... otherTestMethodArgs)
			throws Exception
	{
		PluginTestExecutor.execute(this, output(api, variables, "test-method-succeeded"),
				output(api, variables, "test-method-failed"), () -> variables.updateTask(variables.getStartTask()),
				_ -> null, api, variables, otherTestMethodArgs);
	}

	protected void executeTests(ProcessPluginApi api, Variables variables, Function<Exception, Exception> onError,
			Object... otherTestMethodArgs) throws Exception
	{
		PluginTestExecutor.execute(this, output(api, variables, "test-method-succeeded"),
				output(api, variables, "test-method-failed"), () -> variables.updateTask(variables.getStartTask()),
				onError, api, variables, otherTestMethodArgs);
	}

	private Consumer<String> output(ProcessPluginApi api, Variables variables, String code)
	{
		return t -> variables.getStartTask().addOutput(api.getTaskHelper().createOutput(new StringType(t),
				"http://dsf.dev/fhir/CodeSystem/test", code, api.getProcessPluginDefinition().getResourceVersion()));
	}
}
