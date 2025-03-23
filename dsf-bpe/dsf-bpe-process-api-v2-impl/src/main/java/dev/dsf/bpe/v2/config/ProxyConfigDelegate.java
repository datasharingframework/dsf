package dev.dsf.bpe.v2.config;

import java.util.List;

import dev.dsf.bpe.api.config.BpeProxyConfig;

public class ProxyConfigDelegate implements ProxyConfig
{
	private final BpeProxyConfig delegate;

	public ProxyConfigDelegate(BpeProxyConfig delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public String getUrl()
	{
		return delegate.getUrl();
	}

	@Override
	public boolean isEnabled()
	{
		return delegate.isEnabled();
	}

	@Override
	public boolean isEnabled(String targetUrl)
	{
		return delegate.isEnabled(targetUrl);
	}

	@Override
	public String getUsername()
	{
		return delegate.getUsername();
	}

	@Override
	public char[] getPassword()
	{
		return delegate.getPassword();
	}

	@Override
	public List<String> getNoProxyUrls()
	{
		return delegate.getNoProxyUrls();
	}

	@Override
	public boolean isNoProxyUrl(String url)
	{
		return delegate.isNoProxyUrl(url);
	}
}
