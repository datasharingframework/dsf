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
package dev.dsf.bpe.v2.client.fhir;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;

import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.config.FhirClientConfig;

public class ClientConfigDelegate implements ClientConfig
{
	private final FhirClientConfig delegate;
	private final BpeProxyConfig proxyConfig;

	public ClientConfigDelegate(FhirClientConfig delegate, BpeProxyConfig proxyConfig)
	{
		this.delegate = delegate;
		this.proxyConfig = proxyConfig;
	}

	@Override
	public String getFhirServerId()
	{
		return delegate.fhirServerId();
	}

	@Override
	public String getBaseUrl()
	{
		return delegate.baseUrl();
	}

	@Override
	public boolean isStartupConnectionTestEnabled()
	{
		return delegate.startupConnectionTestEnabled();
	}

	@Override
	public boolean isDebugLoggingEnabled()
	{
		return delegate.debugLoggingEnabled();
	}

	@Override
	public Duration getConnectTimeout()
	{
		return delegate.connectTimeout();
	}

	@Override
	public Duration getReadTimeout()
	{
		return delegate.readTimeout();
	}

	@Override
	public KeyStore getTrustStore()
	{
		return delegate.trustStore();
	}

	@Override
	public CertificateAuthentication getCertificateAuthentication()
	{
		return delegate.certificateAuthentication() == null ? null : new CertificateAuthentication()
		{
			@Override
			public KeyStore getKeyStore()
			{
				return delegate.certificateAuthentication().keyStore();
			}

			@Override
			public char[] getKeyStorePassword()
			{
				return delegate.certificateAuthentication().keyStorePassword();
			}
		};
	}

	@Override
	public BasicAuthentication getBasicAuthentication()
	{
		return delegate.basicAuthentication() == null ? null : new BasicAuthentication()
		{
			@Override
			public String getUsername()
			{
				return delegate.basicAuthentication().username();
			}

			@Override
			public char[] getPassword()
			{
				return delegate.basicAuthentication().password();
			}
		};
	}

	@Override
	public BearerAuthentication getBearerAuthentication()
	{
		return delegate.bearerAuthentication() == null ? null : delegate.bearerAuthentication()::token;
	}

	@Override
	public OidcAuthentication getOidcAuthentication()
	{
		return delegate.oidcAuthentication() == null ? null : new OidcAuthentication()
		{
			@Override
			public boolean isStartupConnectionTestEnabled()
			{
				return delegate.oidcAuthentication().startupConnectionTestEnabled();
			}

			@Override
			public boolean isDebugLoggingEnabled()
			{
				return delegate.debugLoggingEnabled();
			}

			@Override
			public KeyStore getTrustStore()
			{
				return delegate.oidcAuthentication().trustStore();
			}

			@Override
			public Duration getReadTimeout()
			{
				return delegate.oidcAuthentication().readTimeout();
			}

			@Override
			public String getDiscoveryPath()
			{
				return delegate.oidcAuthentication().discoveryPath();
			}

			@Override
			public Duration getConnectTimeout()
			{
				return delegate.oidcAuthentication().connectTimeout();
			}

			@Override
			public char[] getClientSecret()
			{
				return delegate.oidcAuthentication().clientSecret();
			}

			@Override
			public String getClientId()
			{
				return delegate.oidcAuthentication().clientId();
			}

			@Override
			public String getBaseUrl()
			{
				return delegate.oidcAuthentication().baseUrl();
			}

			@Override
			public List<String> getRequiredAudiences()
			{
				return delegate.oidcAuthentication().requiredAudiences();
			}

			@Override
			public boolean isVerifyAuthorizedPartyEnabled()
			{
				return delegate.oidcAuthentication().verifyAuthorizedParty();
			}

			@Override
			public Proxy getProxy()
			{
				return ClientConfigDelegate.getProxy(getBaseUrl(), proxyConfig);
			}
		};
	}

	@Override
	public Proxy getProxy()
	{
		return getProxy(getBaseUrl(), proxyConfig);
	}

	private static Proxy getProxy(String targetUrl, BpeProxyConfig proxyConfig)
	{
		return !proxyConfig.isEnabled(targetUrl) ? null : new Proxy()
		{
			@Override
			public String getUrl()
			{
				return proxyConfig.getUrl();
			}

			@Override
			public String getUsername()
			{
				return proxyConfig.getUsername();
			}

			@Override
			public char[] getPassword()
			{
				return proxyConfig.getPassword();
			}
		};
	}
}
