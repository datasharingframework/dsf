package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.variables.Variables;

public class ApiTest extends AbstractTest
{
	private DelegateExecution execution;

	public ApiTest(ProcessPluginApi api)
	{
		super(api);
	}

	@Override
	protected void doExecute(DelegateExecution execution, Variables variables) throws BpmnError, Exception
	{
		this.execution = execution;

		super.doExecute(execution, variables);

		this.execution = null;
	}

	@PluginTest
	public void apiNotNull() throws Exception
	{
		expectNotNull(api);
	}

	@PluginTest
	public void apiGetEndpointProviderNotNull() throws Exception
	{
		expectNotNull(api.getEndpointProvider());
	}

	@PluginTest
	public void apiGetFhirContextNotNull() throws Exception
	{
		expectNotNull(api.getFhirContext());
	}

	@PluginTest
	public void apiGetFhirWebserviceClientProviderNotNull() throws Exception
	{
		expectNotNull(api.getFhirWebserviceClientProvider());
	}

	@PluginTest
	public void apiGetMailServiceNotNull() throws Exception
	{
		expectNotNull(api.getMailService());
	}

	@PluginTest
	public void apiGetObjectMapperNotNull() throws Exception
	{
		expectNotNull(api.getObjectMapper());
	}

	@PluginTest
	public void apiGetOrganizationProviderNotNull() throws Exception
	{
		expectNotNull(api.getOrganizationProvider());
	}

	@PluginTest
	public void apiGetProcessAuthorizationHelperNotNull() throws Exception
	{
		expectNotNull(api.getProcessAuthorizationHelper());
	}

	@PluginTest
	public void apiGetProxyConfigNotNull() throws Exception
	{
		expectNotNull(api.getProxyConfig());
	}

	@PluginTest
	public void apiGetReadAccessHelperNotNull() throws Exception
	{
		expectNotNull(api.getReadAccessHelper());
	}

	@PluginTest
	public void apiGetTaskHelperNotNull() throws Exception
	{
		expectNotNull(api.getTaskHelper());
	}

	@PluginTest
	public void apiGetVariablesNotNull() throws Exception
	{
		expectNotNull(api.getVariables(execution));
	}
}
