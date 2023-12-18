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
