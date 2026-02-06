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

import java.time.ZonedDateTime;

import dev.dsf.bpe.test.json.JsonPojo;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class JsonVariableTestSet implements ServiceTask
{
	public static final String JSON_VARIABLE = "json-variable";
	public static final String STRING_VARIABLE = "string-variable";
	public static final String INTEGER_VARIABLE = "integer-variable";

	public static final String TEST_VALUE_1 = "test-value-1";
	public static final String TEST_VALUE_2 = "test-value-2";
	public static final ZonedDateTime TEST_ZONED_DATE_TIME_VALUE = ZonedDateTime.now();
	public static final String TEST_STRING = "test-string";
	public static final Integer TEST_INTEGER = 42;

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		variables.setJsonVariable(JSON_VARIABLE, new JsonPojo(TEST_VALUE_1, TEST_VALUE_2, TEST_ZONED_DATE_TIME_VALUE));
		variables.setString(STRING_VARIABLE, TEST_STRING);
		variables.setInteger(INTEGER_VARIABLE, TEST_INTEGER);
	}
}
