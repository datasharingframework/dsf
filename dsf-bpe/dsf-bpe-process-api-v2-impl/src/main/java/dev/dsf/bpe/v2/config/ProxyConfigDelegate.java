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
