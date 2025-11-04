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
package dev.dsf.bpe.v2.error.impl;

import java.util.function.Function;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.values.SendTaskValues;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ExecutionListenerErrorHandler;
import dev.dsf.bpe.v2.error.MessageSendTaskErrorHandler;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.error.UserTaskListenerErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ExceptionToErrorBoundaryEventTranslationErrorHandler implements ServiceTaskErrorHandler,
		MessageSendTaskErrorHandler, ExecutionListenerErrorHandler, UserTaskListenerErrorHandler
{
	public static final Function<Exception, String> DEFAULT_ERROR_MESSAGE_TRANSLATOR = Exception::getMessage;

	private final Function<Exception, String> errorCodeTranslator;
	private final Function<Exception, String> errorMessageTranslator;

	public ExceptionToErrorBoundaryEventTranslationErrorHandler(Function<Exception, String> errorCodeTranslator)
	{
		this(errorCodeTranslator, DEFAULT_ERROR_MESSAGE_TRANSLATOR);
	}

	public ExceptionToErrorBoundaryEventTranslationErrorHandler(Function<Exception, String> errorCodeTranslator,
			Function<Exception, String> errorMessageTranslator)
	{
		this.errorCodeTranslator = errorCodeTranslator;
		this.errorMessageTranslator = errorMessageTranslator;
	}

	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, SendTaskValues sendTaskValues,
			Exception exception)
	{
		return new ErrorBoundaryEvent(errorCodeTranslator.apply(exception), errorMessageTranslator.apply(exception));
	}

	@Override
	public Exception handleException(ProcessPluginApi api, Variables variables, Exception exception)
	{
		return new ErrorBoundaryEvent(errorCodeTranslator.apply(exception), errorMessageTranslator.apply(exception));
	}

	@Override
	public ErrorBoundaryEvent handleErrorBoundaryEvent(ProcessPluginApi api, Variables variables,
			ErrorBoundaryEvent event)
	{
		return event;
	}
}
