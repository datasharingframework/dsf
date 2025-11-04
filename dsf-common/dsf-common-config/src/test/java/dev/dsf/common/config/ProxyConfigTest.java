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
package dev.dsf.common.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ProxyConfigTest
{
	@Test
	public void testBadProxyUrl() throws Exception
	{
		assertNull(new ProxyConfigImpl(null, null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl(" ", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("foo", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("foo:8080", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("ftp://foo", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("http://", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("http:// ", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("ftp://foo:1234", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("http://:1234", null, null, null).getUrl());
		assertNull(new ProxyConfigImpl("http:// :1234", null, null, null).getUrl());

		assertNotNull(new ProxyConfigImpl("http://proxy", null, null, null).getUrl());
		assertNotNull(new ProxyConfigImpl("http://proxy:8080", null, null, null).getUrl());
		assertNotNull(new ProxyConfigImpl("https://proxy", null, null, null).getUrl());
		assertNotNull(new ProxyConfigImpl("https://proxy:8080", null, null, null).getUrl());
	}

	@Test
	public void testGetMethods() throws Exception
	{
		String url = "http://proxy", username = "username";
		char[] password = "password".toCharArray();
		// Arrays.asList as we need a null element, not allowed in List.of
		List<String> noProxy = Arrays.asList(null, " ", "no-proxy");

		ProxyConfigImpl c = new ProxyConfigImpl(url, username, password, noProxy);
		assertEquals(url, c.getUrl());
		assertEquals(username, c.getUsername());
		assertEquals(password, c.getPassword());
		assertNotNull(c.getNoProxyUrls());
		assertEquals(1, c.getNoProxyUrls().size());
		assertEquals("no-proxy", c.getNoProxyUrls().get(0));
	}

	@Test
	public void testIsEnabled() throws Exception
	{
		assertTrue(new ProxyConfigImpl("http://proxy", null, null, null).isEnabled());
		assertTrue(new ProxyConfigImpl("http://proxy", null, null, List.of("foo")).isEnabled());
		assertFalse(new ProxyConfigImpl(null, null, null, null).isEnabled());
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, List.of("*")).isEnabled());
	}

	@Test
	public void testIsEndabled() throws Exception
	{
		ProxyConfig proxyConfig = new ProxyConfigImpl("http://proxy", null, null,
				List.of("foo.bar", "foo.bar.baz:8080", "test:1234"));

		assertFalse(proxyConfig.isEnabled("http://foo.bar"));
		assertFalse(proxyConfig.isEnabled("http://foo.bar:8080"));
		assertFalse(proxyConfig.isEnabled("https://foo.bar"));
		assertFalse(proxyConfig.isEnabled("https://foo.bar:8443"));
		assertFalse(proxyConfig.isEnabled("http://test.foo.bar"));
		assertFalse(proxyConfig.isEnabled("https://test.foo.bar:443"));
		assertFalse(proxyConfig.isEnabled("https://test.test:1234"));
		assertFalse(proxyConfig.isEnabled("http://foo.bar.baz:8080"));
		assertFalse(proxyConfig.isEnabled("https://foo.bar.baz:8080"));

		assertTrue(proxyConfig.isEnabled("http://foo.bar.baz"));
		assertTrue(proxyConfig.isEnabled("https://foo.bar.baz"));
		assertTrue(proxyConfig.isEnabled("http://bar.baz"));
		assertTrue(proxyConfig.isEnabled("https://bar.baz"));
		assertTrue(proxyConfig.isEnabled("https://test.test"));
	}

	@Test
	public void testIsEnabledAllNoProxy() throws Exception
	{
		ProxyConfig proxyConfig = new ProxyConfigImpl(null, null, null, List.of("*"));

		assertFalse(proxyConfig.isEnabled("http://foo.bar"));
		assertFalse(proxyConfig.isEnabled("http://foo.bar:8080"));
		assertFalse(proxyConfig.isEnabled("https://foo.bar"));
		assertFalse(proxyConfig.isEnabled("https://foo.bar:8443"));
		assertFalse(proxyConfig.isEnabled("http://test.foo.bar"));
		assertFalse(proxyConfig.isEnabled("https://test.foo.bar:443"));

		assertFalse(proxyConfig.isEnabled("http://foo.bar.baz:8080"));
		assertFalse(proxyConfig.isEnabled("https://foo.bar.baz:8080"));
		assertFalse(proxyConfig.isEnabled("http://foo.bar.baz"));
		assertFalse(proxyConfig.isEnabled("https://foo.bar.baz"));
		assertFalse(proxyConfig.isEnabled("http://bar.baz"));
		assertFalse(proxyConfig.isEnabled("https://bar.baz"));
	}

	@Test
	public void testIsEnabledNull() throws Exception
	{
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, null).isEnabled(null));
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, List.of("*")).isEnabled(null));
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, null).isEnabled(null));
	}

	@Test
	public void testIsEnabledBlank() throws Exception
	{
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, List.of("*")).isEnabled(""));
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, List.of("*")).isEnabled(" "));
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, null).isEnabled(""));
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, null).isEnabled(" "));
	}

	@Test
	public void testIsEnabledMalformedUrl() throws Exception
	{
		assertFalse(new ProxyConfigImpl("http://proxy", null, null, List.of("*")).isEnabled("malformed"));
		assertTrue(new ProxyConfigImpl("http://proxy", null, null, null).isEnabled(":malformed"));
		assertTrue(new ProxyConfigImpl("http://proxy", null, null, null).isEnabled("malformed"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIsEnabledSchemaNotSupported() throws Exception
	{
		new ProxyConfigImpl("http://proxy", null, null, null).isEnabled("foo://bar");
	}
}
