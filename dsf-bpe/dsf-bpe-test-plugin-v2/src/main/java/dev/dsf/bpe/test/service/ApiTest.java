package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class ApiTest extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void apiNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api);
	}

	@PluginTest
	public void apiGetEndpointProviderNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getEndpointProvider());
	}

	@PluginTest
	public void apiGetFhirContextNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getFhirContext());
	}

	@PluginTest
	public void apiGetDsfClientProviderNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getDsfClientProvider());
	}

	@PluginTest
	public void apiGetFhirClientProviderNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getFhirClientProvider());
	}

	@PluginTest
	public void apiGetOidcClientProviderrNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getOidcClientProvider());
	}

	@PluginTest
	public void apiGetMailServiceNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getMailService());
	}

	@PluginTest
	public void apiGetMimetypeService(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getMimeTypeService());
	}

	@PluginTest
	public void apiGetObjectMapperNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getObjectMapper());
	}

	@PluginTest
	public void apiGetOrganizationProviderNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getOrganizationProvider());
	}

	@PluginTest
	public void apiGetProcessAuthorizationHelperNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getProcessAuthorizationHelper());
	}

	@PluginTest
	public void apiGetProxyConfigNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getProxyConfig());
	}

	@PluginTest
	public void apiGetReadAccessHelperNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getReadAccessHelper());
	}

	@PluginTest
	public void apiGetTaskHelperNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getTaskHelper());
	}
}
