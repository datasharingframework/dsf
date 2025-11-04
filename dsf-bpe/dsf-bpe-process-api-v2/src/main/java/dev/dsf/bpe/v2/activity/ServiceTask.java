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
package dev.dsf.bpe.v2.activity;

import org.hl7.fhir.r4.model.Task;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultServiceTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public interface ServiceTask extends Activity
{
	/**
	 * @param api
	 *            not <code>null</code>
	 * @param variables
	 *            not <code>null</code>
	 * @throws ErrorBoundaryEvent
	 *             to trigger custom error handling flow in BPMN, when using {@link DefaultServiceTaskErrorHandler}
	 * @throws Exception
	 *             to fail the FHIR {@link Task} and stop the process instance, when using
	 *             {@link DefaultServiceTaskErrorHandler}
	 */
	void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception;

	@Override
	default ServiceTaskErrorHandler getErrorHandler()
	{
		return new DefaultServiceTaskErrorHandler();
	}
}
