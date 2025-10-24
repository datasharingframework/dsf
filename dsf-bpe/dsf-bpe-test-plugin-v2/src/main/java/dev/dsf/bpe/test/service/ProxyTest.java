package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectException;
import static dev.dsf.bpe.test.PluginTestExecutor.expectFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;
import static dev.dsf.bpe.test.PluginTestExecutor.expectTrue;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;

public class ProxyTest extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables);
	}

	@PluginTest
	public void isEnabled(ProcessPluginApi api) throws Exception
	{
		expectTrue(api.getProxyConfig().isEnabled());
	}

	@PluginTest
	public void getNoProxyUrls(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getProxyConfig().getNoProxyUrls());
		expectSame(2, api.getProxyConfig().getNoProxyUrls().size());
		expectTrue(api.getProxyConfig().getNoProxyUrls().contains("localhost"));
		expectTrue(api.getProxyConfig().getNoProxyUrls().contains("noproxy:443"));
	}

	@PluginTest
	public void getPassword(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getProxyConfig().getPassword());
		expectSame("proxy_password".toCharArray(), api.getProxyConfig().getPassword());
	}

	@PluginTest
	public void getUrl(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getProxyConfig().getUrl());
		expectSame("http://proxy:8080", api.getProxyConfig().getUrl());
	}

	@PluginTest
	public void getUsername(ProcessPluginApi api) throws Exception
	{
		expectNotNull(api.getProxyConfig().getUsername());
		expectSame("proxy_username", api.getProxyConfig().getUsername());
	}

	@PluginTest
	public void isNotProxyUrl(ProcessPluginApi api) throws Exception
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
