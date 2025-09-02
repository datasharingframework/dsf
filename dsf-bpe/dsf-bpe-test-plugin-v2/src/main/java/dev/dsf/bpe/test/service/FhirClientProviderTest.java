package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import org.hl7.fhir.r4.model.CapabilityStatement;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.service.FhirClientProvider;
import dev.dsf.bpe.v2.variables.Variables;

public class FhirClientProviderTest extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, api.getFhirClientProvider());
	}

	@PluginTest
	public void getFhirClientProviderNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getFhirClientProvider());
	}

	@PluginTest
	public void getClientWithConfiguredServerId(FhirClientProvider clientProvider) throws Exception
	{
		expectNotNull(clientProvider.getClient("dsf-fhir-server"));
		expectTrue(clientProvider.getClient("dsf-fhir-server").isPresent());
	}

	@PluginTest
	public void getClientWithNotConfiguredServerId(FhirClientProvider clientProvider) throws Exception
	{
		expectNotNull(clientProvider.getClient("not-configured"));
		expectFalse(clientProvider.getClient("not-configured").isPresent());
	}

	@PluginTest
	public void getClientConfigTestConnection(FhirClientProvider clientProvider) throws Exception
	{
		clientProvider.getClient("dsf-fhir-server").ifPresent(client ->
		{
			CapabilityStatement statement = client.capabilities().ofType(CapabilityStatement.class).execute();
			expectNotNull(statement);
			expectSame("Data Sharing Framework", statement.getSoftware().getName());
		});
	}

	@PluginTest
	public void getClientConfigTestConnectionViaEndpointIdentifier(FhirClientProvider clientProvider) throws Exception
	{
		clientProvider.getClient("#Test_Endpoint").ifPresent(client ->
		{
			CapabilityStatement statement = client.capabilities().ofType(CapabilityStatement.class).execute();
			expectNotNull(statement);
			expectSame("Data Sharing Framework", statement.getSoftware().getName());
		});
	}
}
