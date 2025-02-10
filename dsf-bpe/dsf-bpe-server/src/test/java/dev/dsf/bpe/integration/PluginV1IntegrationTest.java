package dev.dsf.bpe.integration;

import org.junit.BeforeClass;
import org.junit.Test;

public class PluginV1IntegrationTest extends AbstractPluginIntegrationTest
{
	private static final String PROCESS_VERSION = "1.0";

	public PluginV1IntegrationTest()
	{
		super(PROCESS_VERSION);
	}

	@BeforeClass
	public static void verifyProcessPluginResourcesExist() throws Exception
	{
		verifyProcessPluginResourcesExistForVersion(PROCESS_VERSION);
	}

	@Test
	public void startTestProcess() throws Exception
	{
		executePluginTest(createTestTask("ApiTest"));
	}

	@Test
	public void startProxyTestProcess() throws Exception
	{
		executePluginTest(createTestTask("ProxyTest"));
	}
}