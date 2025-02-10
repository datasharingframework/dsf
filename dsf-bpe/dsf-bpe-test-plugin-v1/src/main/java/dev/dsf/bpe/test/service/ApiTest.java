package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.isNotNull;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.variables.Variables;

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
		isNotNull(api);
	}

	@PluginTest
	public void apiGetEndpointProviderNotNull() throws Exception
	{
		isNotNull(api.getEndpointProvider());
	}

	@PluginTest
	public void apiGetFhirContextNotNull() throws Exception
	{
		isNotNull(api.getFhirContext());
	}

	@PluginTest
	public void apiGetFhirWebserviceClientProviderNotNull() throws Exception
	{
		isNotNull(api.getFhirWebserviceClientProvider());
	}

	@PluginTest
	public void apiGetMailServiceNotNull() throws Exception
	{
		isNotNull(api.getMailService());
	}

	@PluginTest
	public void apiGetObjectMapperNotNull() throws Exception
	{
		isNotNull(api.getObjectMapper());
	}

	@PluginTest
	public void apiGetOrganizationProviderNotNull() throws Exception
	{
		isNotNull(api.getOrganizationProvider());
	}

	@PluginTest
	public void apiGetProcessAuthorizationHelperNotNull() throws Exception
	{
		isNotNull(api.getProcessAuthorizationHelper());
	}

	@PluginTest
	public void apiGetProxyConfigNotNull() throws Exception
	{
		isNotNull(api.getProxyConfig());
	}

	@PluginTest
	public void apiGetReadAccessHelperNotNull() throws Exception
	{
		isNotNull(api.getReadAccessHelper());
	}

	@PluginTest
	public void apiGetTaskHelperNotNull() throws Exception
	{
		isNotNull(api.getTaskHelper());
	}

	@PluginTest
	public void apiGetVariablesNotNull() throws Exception
	{
		isNotNull(api.getVariables(execution));
	}
}
