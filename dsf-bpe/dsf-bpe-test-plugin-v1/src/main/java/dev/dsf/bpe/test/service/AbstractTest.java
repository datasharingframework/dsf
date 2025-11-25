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
				output(variables, "test-method-failed"), () -> variables.updateTask(variables.getStartTask()),
				_ -> null, execution, variables);
	}

	private Consumer<String> output(Variables variables, String code)
	{
		return t -> variables.getStartTask().addOutput(
				api.getTaskHelper().createOutput(new StringType(t), "http://dsf.dev/fhir/CodeSystem/test", code));
	}
}
