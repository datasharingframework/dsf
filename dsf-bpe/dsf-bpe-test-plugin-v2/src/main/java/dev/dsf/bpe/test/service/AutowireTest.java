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

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.test.TestProcessPluginDefinition;
import dev.dsf.bpe.test.autowire.DemoService;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class AutowireTest extends AbstractTest implements ServiceTask
{
	private final DemoService demoService;
	private final TestProcessPluginDefinition pluginDefinition;

	public AutowireTest(DemoService demoService, TestProcessPluginDefinition pluginDefinition)
	{
		this.demoService = demoService;
		this.pluginDefinition = pluginDefinition;
	}

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void demoServiceNotNull() throws Exception
	{
		expectNotNull(demoService);
	}

	@PluginTest
	public void testProcessPluginDefinitionNotNull() throws Exception
	{
		expectNotNull(pluginDefinition);
	}
}
