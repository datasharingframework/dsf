package dev.dsf.common.config;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class ProxyConfigImpl implements ProxyConfig, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProxyConfigImpl.class);

	private final String url;
	private final String username;
	private final char[] password;
	private final List<String> noProxyUrls = new ArrayList<>();

	public ProxyConfigImpl(String url, String username, char[] password, Collection<String> noProxyUrls)
	{
		this.url = nullIfUrlInvalid(url);
		this.username = username;
		this.password = password;

		if (noProxyUrls != null)
			this.noProxyUrls.addAll(noProxyUrls.stream().filter(s -> s != null && !s.isBlank()).toList());

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
				password != null ? "***" : "null", noProxyUrls);
	}

	@Override
	public String getUrl()
	{
		return url;
	}

	@Override
	public boolean isEnabled()
	{
		return url != null && !noProxyUrls.contains("*");
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
		return Collections.unmodifiableList(noProxyUrls);
	}

	@Override
	public boolean isNoProxyUrl(String targetUrl)
	{
		if (noProxyUrls.contains("*"))
			return true;

		if (targetUrl == null || targetUrl.isBlank())
			return false;

		try
		{
			URI u = new URI(targetUrl);

			String host = u.getHost();
			if (host == null)
			{
				logger.debug("Given targetUrl '{}' is malformed, no host value", targetUrl);
				return false;
			}

			String subHost = Stream.of(u.getHost().split("\\.")).skip(1).collect(Collectors.joining("."));
			int port = u.getPort() == -1 ? getDefaultPort(u.getScheme()) : u.getPort();

			return noProxyUrls.stream().anyMatch(s -> s.equals(host) || s.equals(host + ":" + port) || s.equals(subHost)
					|| s.equals(subHost + ":" + port));
		}
		catch (URISyntaxException e)
		{
			logger.debug("Given targetUrl '{}' is malformed: {}", targetUrl, e.getMessage());
			return false;
		}
	}

	private int getDefaultPort(String scheme)
	{
		return switch (scheme)
		{
			case "http", "ws" -> 80;
			case "https", "wss" -> 443;
			default -> throw new IllegalArgumentException("Schema " + scheme + " not supported");
		};
	}
}
