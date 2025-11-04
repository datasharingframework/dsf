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
package dev.dsf.bpe.v2.service;

import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import dev.dsf.bpe.api.service.BpeOidcClientProvider;
import dev.dsf.bpe.v2.client.fhir.ClientConfig.OidcAuthentication;
import dev.dsf.bpe.v2.client.oidc.OidcClient;
import dev.dsf.bpe.v2.client.oidc.OidcClientDelegate;

public class OidcClientProviderDelegate implements OidcClientProvider
{
	private final class OidcAuthenticationApiDelegate
			implements dev.dsf.bpe.api.config.FhirClientConfig.OidcAuthentication
	{
		final OidcAuthentication delegate;

		OidcAuthenticationApiDelegate(OidcAuthentication delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public String baseUrl()
		{
			return delegate.getBaseUrl();
		}

		@Override
		public String discoveryPath()
		{
			return delegate.getDiscoveryPath();
		}

		@Override
		public boolean startupConnectionTestEnabled()
		{
			return delegate.isStartupConnectionTestEnabled();
		}

		@Override
		public boolean debugLoggingEnabled()
		{
			return delegate.isDebugLoggingEnabled();
		}

		@Override
		public Duration connectTimeout()
		{
			return delegate.getConnectTimeout();
		}

		@Override
		public Duration readTimeout()
		{
			return delegate.getReadTimeout();
		}

		@Override
		public KeyStore trustStore()
		{
			return delegate.getTrustStore();
		}

		@Override
		public String clientId()
		{
			return delegate.getClientId();
		}

		@Override
		public char[] clientSecret()
		{
			return delegate.getClientSecret();
		}

		@Override
		public List<String> requiredAudiences()
		{
			return delegate.getRequiredAudiences();
		}

		@Override
		public boolean verifyAuthorizedParty()
		{
			return delegate.isVerifyAuthorizedPartyEnabled();
		}
	}

	private final BpeOidcClientProvider delegate;

	public OidcClientProviderDelegate(BpeOidcClientProvider delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public OidcClient getOidcClient(String baseUrl, String clientId, char[] clientSecret, String discoveryPath,
			Duration connectTimeout, Duration readTimeout, KeyStore trustStore, Boolean enableDebugLogging,
			List<String> requiredAudiences, Boolean verifyAuthorizedParty)
	{
		Objects.requireNonNull(baseUrl, "baseUrl");
		Objects.requireNonNull(clientId, "clientId");
		Objects.requireNonNull(clientSecret, "clientSecret");

		return new OidcClientDelegate(delegate.getOidcClient(baseUrl, clientId, clientSecret, discoveryPath,
				connectTimeout, readTimeout, trustStore, enableDebugLogging, requiredAudiences, verifyAuthorizedParty));
	}

	@Override
	public OidcClient getOidcClient(OidcAuthentication config)
	{
		Objects.requireNonNull(config, "config");

		return new OidcClientDelegate(delegate.getOidcClient(new OidcAuthenticationApiDelegate(config)));
	}
}
