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
package dev.dsf.bpe.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.BeforeClass;
import org.junit.Test;

import dev.dsf.bpe.api.plugin.ProcessPlugin;

public class PluginV1IntegrationTest extends AbstractPluginIntegrationTest
{
	private static final String PROCESS_VERSION = "1.0";

	public PluginV1IntegrationTest()
	{
		super(PROCESS_VERSION);
	}

	@BeforeClass
	public static void verifyProcessPluginResourcesExistAndListenerCalled() throws Exception
	{
		verifyProcessPluginResourcesExistForVersion(PROCESS_VERSION);

		Optional<ProcessPlugin> processPlugin = getProcessPluginForTestProcess(PROCESS_VERSION);
		assertTrue(processPlugin.isPresent());

		// not statically typed since listener class is loaded by plugin class loader
		Object listener = processPlugin.get().getApplicationContext().getBean("processPluginDeploymentStateListener");
		assertNotNull(listener);

		@SuppressWarnings("unchecked")
		List<Boolean> ok = (List<Boolean>) listener.getClass().getMethod("getOk").invoke(listener);
		assertNotNull(ok);
		assertEquals(1, ok.size());
		assertTrue(ok.get(0));
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
	public void startEnvironmentVariableTest() throws Exception
	{
		executePluginTest(createTestTask("EnvironmentVariableTest"));
	}
}