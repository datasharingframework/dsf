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
package dev.dsf.bpe.test.listener;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ExecutionListener;
import dev.dsf.bpe.v2.variables.Variables;

public class StartSendTaskTestListener implements ExecutionListener
{
	public static final String TEST_VARIABLE_VALUE = "testVariableValue";

	@Override
	public void notify(ProcessPluginApi api, Variables variables) throws Exception
	{
		variables.setString("testVariable", TEST_VARIABLE_VALUE);
	}
}
