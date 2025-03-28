package dev.dsf.bpe.integration;

import org.junit.BeforeClass;
import org.junit.Test;

public class PluginV2IntegrationTest extends AbstractPluginIntegrationTest
{
	private static final String PROCESS_VERSION = "2.0";

	public PluginV2IntegrationTest()
	{
		super(PROCESS_VERSION);
	}

	@BeforeClass
	public static void verifyProcessPluginResourcesExist() throws Exception
	{
		verifyProcessPluginResourcesExistForVersion(PROCESS_VERSION);
	}

	@Test
	public void startApiTest() throws Exception
	{
		executePluginTest(createTestTask("Api"));
	}

	@Test
	public void startProxyTest() throws Exception
	{
		executePluginTest(createTestTask("Proxy"));
	}

	@Test
	public void startOrganizationProviderTest() throws Exception
	{
		executePluginTest(createTestTask("OrganizationProvider"));
	}

	@Test
	public void startEndpointProviderTest() throws Exception
	{
		executePluginTest(createTestTask("EndpointProvider"));
	}

	@Test
	public void startFhirClientProviderTest() throws Exception
	{
		executePluginTest(createTestTask("FhirClientProvider"));
	}

	@Test
	public void startSendTaskTest() throws Exception
	{
		executePluginTest(createTestTask("SendTaskTest"));
	}

	@Test
	public void startFieldInjectionTest() throws Exception
	{
		executePluginTest(createTestTask("FieldInjectionTest"));
	}

	@Test
	public void startErrorBoundaryEventTest() throws Exception
	{
		executePluginTest(createTestTask("ErrorBoundaryEventTest"));
	}

	@Test
	public void startExceptionTest() throws Exception
	{
		executePluginTest(createTestTask("ExceptionTest"));
	}

	@Test
	public void startContinueSendTest() throws Exception
	{
		executePluginTest(createTestTask("ContinueSendTest"));
	}

	@Test
	public void startJsonVariableTest() throws Exception
	{
		executePluginTest(createTestTask("JsonVariableTest"));
	}

	@Test
	public void startMimetypeServiceTest() throws Exception
	{
		executePluginTest(createTestTask("MimetypeServiceTest"));
	}
}