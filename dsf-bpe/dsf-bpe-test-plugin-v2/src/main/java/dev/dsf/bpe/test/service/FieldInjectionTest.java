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

import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.test.listener.StartFieldInjectionTestListener;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class FieldInjectionTest extends AbstractTest implements ServiceTask
{
	private String stringField;
	private String stringFieldFromExpression;
	private int intField;

	public void setStringField(String stringField)
	{
		this.stringField = stringField;
	}

	public void setStringFieldFromExpression(String stringFieldFromExpression)
	{
		this.stringFieldFromExpression = stringFieldFromExpression;
	}

	public void setIntField(int intField)
	{
		this.intField = intField;
	}

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void testStringField(ProcessPluginApi api) throws Exception
	{
		expectSame("stringFieldValue", stringField);
	}

	@PluginTest
	public void testStringFieldFromExpression(ProcessPluginApi api) throws Exception
	{
		expectSame("testVariableValue", stringFieldFromExpression);
	}

	@PluginTest
	public void testIntgField(ProcessPluginApi api) throws Exception
	{
		expectSame(StartFieldInjectionTestListener.INT_VARIABLE_VALUE, intField);
	}
}
