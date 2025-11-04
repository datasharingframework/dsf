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

import static dev.dsf.bpe.test.PluginTestExecutor.expectException;
import static dev.dsf.bpe.test.PluginTestExecutor.expectFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v1.ProcessPluginApi;

public class ProxyTest extends AbstractTest
{
	public ProxyTest(ProcessPluginApi api)
	{
		super(api);
	}

	@PluginTest
	public void isEnabled() throws Exception
	{
		expectTrue(api.getProxyConfig().isEnabled());
	}

	@PluginTest
	public void getNoProxyUrls() throws Exception
	{
		expectNotNull(api.getProxyConfig().getNoProxyUrls());
		expectSame(2, api.getProxyConfig().getNoProxyUrls().size());
		expectTrue(api.getProxyConfig().getNoProxyUrls().contains("localhost"));
		expectTrue(api.getProxyConfig().getNoProxyUrls().contains("noproxy:443"));
	}

	@PluginTest
	public void getPassword() throws Exception
	{
		expectNotNull(api.getProxyConfig().getPassword());
		expectSame("proxy_password".toCharArray(), api.getProxyConfig().getPassword());
	}

	@PluginTest
	public void getUrl() throws Exception
	{
		expectNotNull(api.getProxyConfig().getUrl());
		expectSame("http://proxy:8080", api.getProxyConfig().getUrl());
	}

	@PluginTest
	public void getUsername() throws Exception
	{
		expectNotNull(api.getProxyConfig().getUsername());
		expectSame("proxy_username", api.getProxyConfig().getUsername());
	}

	@PluginTest
	public void isNotProxyUrl() throws Exception
	{
		expectTrue(api.getProxyConfig().isNoProxyUrl("https://localhost"));
		expectTrue(api.getProxyConfig().isNoProxyUrl("http://localhost"));
		expectTrue(api.getProxyConfig().isNoProxyUrl("http://localhost:8080"));
		expectException(IllegalArgumentException.class, () -> api.getProxyConfig().isNoProxyUrl("localhost:1234"));
		expectException(IllegalArgumentException.class, () -> api.getProxyConfig().isNoProxyUrl("ftp://localhost"));

		expectTrue(api.getProxyConfig().isNoProxyUrl("https://noproxy"));
		expectFalse(api.getProxyConfig().isNoProxyUrl("http://noproxy"));
		expectFalse(api.getProxyConfig().isNoProxyUrl("http://noproxy:8080"));

		expectFalse(api.getProxyConfig().isNoProxyUrl("foo"));
	}
}
