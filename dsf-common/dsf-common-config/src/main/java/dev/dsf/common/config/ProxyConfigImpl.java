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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.config.network.HostSpecParser.HostSpec;
import dev.dsf.common.config.network.InetSocketAddressMatcher;
import dev.dsf.common.config.network.InetSocketAddressMatcherList;
import dev.dsf.common.config.network.StaticCidrMatcher;
import dev.dsf.common.config.network.StaticHostnameMatcher;

public class ProxyConfigImpl implements ProxyConfig, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProxyConfigImpl.class);

	private static final Pattern IPV4_LITERAL = Pattern.compile("^[0-9]{1,3}(?:\\.[0-9]{1,3}){3}$");

	private final String url;
	private final String username;
	private final char[] password;

	private final List<HostSpec> noProxyUrls;
	private final InetSocketAddressMatcher noProxyMatcher;

	public ProxyConfigImpl(String url, String username, char[] password, Collection<HostSpec> noProxyUrls)
	{
		this.url = nullIfUrlInvalid(url);
		this.username = username;
		this.password = password;

		this.noProxyUrls = noProxyUrls == null ? List.of() : List.copyOf(noProxyUrls);

		noProxyMatcher = new InetSocketAddressMatcherList(this.noProxyUrls.stream().map(ProxyConfigImpl::toMatcher));
	}

	private static InetSocketAddressMatcher toMatcher(HostSpec hostSpec)
	{
		if (hostSpec.isIp())
			return StaticCidrMatcher.of(hostSpec);
		else if (hostSpec.isDomainOrWildcard())
			return StaticHostnameMatcher.of(hostSpec);
		else
			throw new IllegalArgumentException("hostSpec not supported");
	}

	private static String nullIfUrlInvalid(String url)
	{
		if (url == null)
			return null;

		try
		{
			URL u = new URI(url).toURL();
			if (u.getHost() == null || u.getHost().isBlank())
			{
				logger.warn("Forward proxy url '{}' malformed: no host name", u);
				return null;
			}
			else if (!"http".equals(u.getProtocol()) && !"https".equals(u.getProtocol()))
			{
				logger.warn("Forward proxy url '{}' malformed: protocol not http or https", u);
				return null;
			}

			return url;
		}
		catch (IllegalArgumentException | MalformedURLException | URISyntaxException e)
		{
			logger.warn("Forward proxy url '{}' malformed: {}", url, e.getMessage());
			return null;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		logger.info("Forward proxy config: {url: {}, username: {}, password: {}, no-proxy: {}}", url, username,
				password != null ? "***" : "null", noProxyMatcher.toString());
	}

	@Override
	public String getUrl()
	{
		return url;
	}

	@Override
	public boolean isEnabled()
	{
		return url != null;
	}

	@Override
	public boolean isEnabled(String targetUrl)
	{
		if (targetUrl == null || targetUrl.isBlank())
			return false;

		return isEnabled() && !isNoProxyUrl(targetUrl);
	}

	@Override
	public String getUsername()
	{
		return username;
	}

	@Override
	public char[] getPassword()
	{
		return password;
	}

	@Override
	public List<String> getNoProxyUrls()
	{
		return noProxyUrls.stream().map(HostSpec::toString).toList();
	}

	@Override
	public boolean isNoProxyUrl(String targetUrl)
	{
		if (targetUrl == null || targetUrl.isBlank())
			return false;

		Optional<InetSocketAddress> targetAddress = toSocketAddress(targetUrl);
		if (targetAddress.isEmpty())
			return false;
		else
			return noProxyMatcher.matches(targetAddress.get());
	}

	private boolean isLikelyIpLiteral(String host)
	{
		return host.indexOf(':') >= 0 || IPV4_LITERAL.matcher(host).matches();
	}

	private Optional<InetSocketAddress> toSocketAddress(String targetUrl)
	{
		try
		{
			URI u = new URI(targetUrl);

			String scheme = u.getScheme();
			String host = u.getHost();

			if (scheme == null || host == null)
			{
				logger.debug("Invalid target URL: scheme null or host null");
				return Optional.empty();
			}

			int port = u.getPort() >= 0 ? u.getPort() : getDefaultPort(scheme);

			if (isLikelyIpLiteral(host))
				return Optional.of(new InetSocketAddress(InetAddress.ofLiteral(host), port));
			else
				return Optional.of(InetSocketAddress.createUnresolved(host, port));
		}
		catch (URISyntaxException | IllegalArgumentException e)
		{
			logger.debug("Invalid target URL: {}", e.getMessage());
			return Optional.empty();
		}
	}

	private int getDefaultPort(String scheme)
	{
		return switch (scheme)
		{
			case "http", "ws" -> 80;
			case "https", "wss" -> 443;
			default -> throw new IllegalArgumentException("Scheme '" + scheme + "' not supported");
		};
	}
}
