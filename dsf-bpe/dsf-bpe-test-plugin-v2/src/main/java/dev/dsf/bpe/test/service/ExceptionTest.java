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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.error.ServiceTaskErrorHandler;
import dev.dsf.bpe.v2.error.impl.DefaultServiceTaskErrorHandler;
import dev.dsf.bpe.v2.variables.Variables;

public class ExceptionTest implements ServiceTask
{
	private static final Logger logger = LoggerFactory.getLogger(ExceptionTest.class);

	private static final String EXCEPTION_TEST_MESSAGE = "TestMessage";

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		throw new RuntimeException(EXCEPTION_TEST_MESSAGE);
	}

	@Override
	public ServiceTaskErrorHandler getErrorHandler()
	{
		return new DefaultServiceTaskErrorHandler()
		{
			@Override
			public Exception handleException(ProcessPluginApi api, Variables variables, Exception exception)
			{
				if (EXCEPTION_TEST_MESSAGE.equals(exception.getMessage()))
				{
					logger.info("Handling expected exception", exception);
					return null;
				}
				else
				{
					logger.warn("Handling unexpected exception", exception);
					return exception;
				}
			}
		};
	}
}
