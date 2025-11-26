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

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.test.TestProcessPluginDefinition;
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
	public void apiGetProcessPluginDefinitionNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getProcessPluginDefinition());

		expectSame(TestProcessPluginDefinition.NAME, api.getProcessPluginDefinition().getName());
		expectSame(TestProcessPluginDefinition.RELEASE_DATE, api.getProcessPluginDefinition().getReleaseDate());
		expectSame(TestProcessPluginDefinition.RELEASE_DATE, api.getProcessPluginDefinition().getResourceReleaseDate());
		expectSame(TestProcessPluginDefinition.VERSION, api.getProcessPluginDefinition().getVersion());
		expectSame(TestProcessPluginDefinition.VERSION.substring(0, 3),
				api.getProcessPluginDefinition().getResourceVersion());
	}

	@PluginTest
	public void apiGetProxyConfigNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getProxyConfig());
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
	public void apiGetFhirClientConfigProviderNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getFhirClientConfigProvider());
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
	public void apiGetMimeTypeService(ProcessPluginApi api) throws Exception
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
	public void apiGetQuestionnaireResponseHelperNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getQuestionnaireResponseHelper());
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

	@PluginTest
	public void apiGetCompressionServiceNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getCompressionService());
	}

	@PluginTest
	public void apiGetCryptoServiceNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getCryptoService());
	}

	@PluginTest
	public void apiGetTargetProviderNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getTargetProvider());
	}

	@PluginTest
	public void apiGetDataLoggerNotNull(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getDataLogger());
	}
}
