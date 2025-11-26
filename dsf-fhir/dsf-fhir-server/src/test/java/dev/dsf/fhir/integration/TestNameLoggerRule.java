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
package dev.dsf.fhir.integration;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestNameLoggerRule extends TestWatcher
{
	private static final Logger logger = LoggerFactory.getLogger(TestNameLoggerRule.class);

	@Override
	protected void starting(Description description)
	{
		logger.info("Starting {}.{} ...", description.getClassName(), description.getMethodName());
	}

	@Override
	protected void succeeded(Description description)
	{
		logger.info("{}.{} [succeeded]", description.getClassName(), description.getMethodName());
	}

	@Override
	protected void failed(Throwable e, Description description)
	{
		logger.info("{}.{} [failed]", description.getClassName(), description.getMethodName());
	}
}
