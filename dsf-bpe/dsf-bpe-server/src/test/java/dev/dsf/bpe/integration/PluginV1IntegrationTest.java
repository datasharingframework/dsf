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
	public void startApiTestProcess() throws Exception
	{
		executePluginTest(createTestTask("Api"));
	}

	@Test
	public void startProxyTestProcess() throws Exception
	{
		executePluginTest(createTestTask("Proxy"));
	}

	@Test
	public void startOrganizationProviderTestProcess() throws Exception
	{
		executePluginTest(createTestTask("OrganizationProvider"));
	}

	@Test
	public void startEndpointProviderTestProcess() throws Exception
	{
		executePluginTest(createTestTask("EndpointProvider"));
	}
}