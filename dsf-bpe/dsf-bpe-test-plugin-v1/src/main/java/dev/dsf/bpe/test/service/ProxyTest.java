package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectException;
import static dev.dsf.bpe.test.PluginTestExecutor.isFalse;
import static dev.dsf.bpe.test.PluginTestExecutor.isNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.isSame;
import static dev.dsf.bpe.test.PluginTestExecutor.isTrue;

import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v1.ProcessPluginApi;

public class ProxyTest extends AbstractTest
{
	public ProxyTest(ProcessPluginApi api)
	{
		super(api);
	}

	@PluginTest
	public void testGetProxyConfigNotNull() throws Exception
	{
		isNotNull(api.getProxyConfig());
	}

	@PluginTest
	public void testGetProxyConfigProxyEnabled() throws Exception
	{
		isTrue(api.getProxyConfig().isEnabled());
	}

	@PluginTest
	public void testGetNoProxyUrls() throws Exception
	{
		isNotNull(api.getProxyConfig().getNoProxyUrls());
		isSame(2, api.getProxyConfig().getNoProxyUrls().size());
		isTrue(api.getProxyConfig().getNoProxyUrls().contains("localhost"));
		isTrue(api.getProxyConfig().getNoProxyUrls().contains("noproxy:443"));
	}

	@PluginTest
	public void testGetPassword() throws Exception
	{
		isNotNull(api.getProxyConfig().getPassword());
		isSame("proxy_password".toCharArray(), api.getProxyConfig().getPassword());
	}

	@PluginTest
	public void testGetUrl() throws Exception
	{
		isNotNull(api.getProxyConfig().getUrl());
		isSame("http://proxy:8080", api.getProxyConfig().getUrl());
	}

	@PluginTest
	public void testGetUsername() throws Exception
	{
		isNotNull(api.getProxyConfig().getUsername());
		isSame("proxy_username", api.getProxyConfig().getUsername());
	}

	@PluginTest
	public void testIsNotProxyUrl() throws Exception
	{
		isTrue(api.getProxyConfig().isNoProxyUrl("https://localhost"));
		isTrue(api.getProxyConfig().isNoProxyUrl("http://localhost"));
		isTrue(api.getProxyConfig().isNoProxyUrl("http://localhost:8080"));
		expectException(IllegalArgumentException.class, () -> api.getProxyConfig().isNoProxyUrl("localhost:1234"));
		expectException(IllegalArgumentException.class, () -> api.getProxyConfig().isNoProxyUrl("ftp://localhost"));

		isTrue(api.getProxyConfig().isNoProxyUrl("https://noproxy"));
		isFalse(api.getProxyConfig().isNoProxyUrl("http://noproxy"));
		isFalse(api.getProxyConfig().isNoProxyUrl("http://noproxy:8080"));

		isFalse(api.getProxyConfig().isNoProxyUrl("foo"));
	}
}
