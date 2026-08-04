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
package dev.dsf.bpe.test.message;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.MessageSendTask;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.error.MessageSendTaskErrorHandler;
import dev.dsf.bpe.v2.error.impl.ExceptionToErrorBoundaryEventTranslationErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class SendTaskErrorBoundaryEventTestThrow extends AbstractTest implements MessageSendTask
{
	public static final String TEST_ERROR_CODE = "testErrorCode";
	public static final String TEST_ERROR_MESSAGE = "testErrorMessage";

	@Override
	public void execute(ProcessPluginApi api, Variables variables, SendTaskValues sendTask) throws Exception
	{
		throw new RuntimeException(TEST_ERROR_MESSAGE);
	}

	@Override
	public MessageSendTaskErrorHandler getErrorHandler()
	{
		return new ExceptionToErrorBoundaryEventTranslationErrorHandler(_ -> TEST_ERROR_CODE);
	}
}
