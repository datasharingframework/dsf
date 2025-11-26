/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
