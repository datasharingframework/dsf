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
package dev.dsf.bpe.v2.client.oidc;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dev.dsf.bpe.v2.client.oidc.Jwks.JwksKey;

public class OidcClientDelegate implements OidcClient
{
	private static final class ConfigurationApiDelegate implements Configuration
	{
		final dev.dsf.bpe.api.client.oidc.Configuration delegate;

		ConfigurationApiDelegate(dev.dsf.bpe.api.client.oidc.Configuration delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public String getTokenEndpoint()
		{
			return delegate.getTokenEndpoint();
		}

		@Override
		public String getJwksUri()
		{
			return delegate.getJwksUri();
		}

		@Override
		public String getIssuer()
		{
			return delegate.getIssuer();
		}

		@Override
		public Set<String> getGrantTypesSupported()
		{
			return delegate.getGrantTypesSupported();
		}
	}

	private static final class ConfigurationV2Delegate implements dev.dsf.bpe.api.client.oidc.Configuration
	{
		final Configuration delegate;

		ConfigurationV2Delegate(Configuration delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public String getTokenEndpoint()
		{
			return delegate.getTokenEndpoint();
		}

		@Override
		public String getJwksUri()
		{
			return delegate.getJwksUri();
		}

		@Override
		public String getIssuer()
		{
			return delegate.getIssuer();
		}

		@Override
		public Set<String> getGrantTypesSupported()
		{
			return delegate.getGrantTypesSupported();
		}
	}

	private static final class JwksApiDelegate implements Jwks
	{
		final dev.dsf.bpe.api.client.oidc.Jwks delegate;

		JwksApiDelegate(dev.dsf.bpe.api.client.oidc.Jwks delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public Set<JwksKey> getKeys()
		{
			return delegate.getKeys().stream().map(JwksKeyApiDelegate::new).collect(Collectors.toUnmodifiableSet());
		}

		@Override
		public Optional<JwksKey> getKey(String kid)
		{
			return delegate.getKey(kid).map(JwksKeyApiDelegate::new);
		}
	}

	private static final class JwksV2Delegate implements dev.dsf.bpe.api.client.oidc.Jwks
	{
		final Jwks delegate;

		JwksV2Delegate(Jwks delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public Set<JwksKey> getKeys()
		{
			return delegate.getKeys().stream().map(JwksKeyV2Delegate::new).collect(Collectors.toUnmodifiableSet());
		}

		@Override
		public Optional<JwksKey> getKey(String kid)
		{
			return delegate.getKey(kid).map(JwksKeyV2Delegate::new);
		}
	}

	private static final class JwksKeyApiDelegate implements JwksKey
	{
		final dev.dsf.bpe.api.client.oidc.Jwks.JwksKey delegate;

		JwksKeyApiDelegate(dev.dsf.bpe.api.client.oidc.Jwks.JwksKey delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public String getKid()
		{
			return delegate.getKid();
		}

		@Override
		public String getKty()
		{
			return delegate.getKty();
		}

		@Override
		public String getAlg()
		{
			return delegate.getAlg();
		}

		@Override
		public String getCrv()
		{
			return delegate.getCrv();
		}

		@Override
		public String getUse()
		{
			return delegate.getUse();
		}

		@Override
		public String getN()
		{
			return delegate.getN();
		}

		@Override
		public String getE()
		{
			return delegate.getE();
		}

		@Override
		public String getX()
		{
			return delegate.getX();
		}

		@Override
		public String getY()
		{
			return delegate.getY();
		}
	}

	private static final class JwksKeyV2Delegate implements dev.dsf.bpe.api.client.oidc.Jwks.JwksKey
	{
		final JwksKey delegate;

		JwksKeyV2Delegate(JwksKey delegate)
		{
			this.delegate = delegate;
		}

		@Override
		public String getKid()
		{
			return delegate.getKid();
		}

		@Override
		public String getKty()
		{
			return delegate.getKty();
		}

		@Override
		public String getAlg()
		{
			return delegate.getAlg();
		}

		@Override
		public String getCrv()
		{
			return delegate.getCrv();
		}

		@Override
		public String getUse()
		{
			return delegate.getUse();
		}

		@Override
		public String getN()
		{
			return delegate.getN();
		}

		@Override
		public String getE()
		{
			return delegate.getE();
		}

		@Override
		public String getX()
		{
			return delegate.getX();
		}

		@Override
		public String getY()
		{
			return delegate.getY();
		}
	}

	private final dev.dsf.bpe.api.client.oidc.OidcClient delegate;

	public OidcClientDelegate(dev.dsf.bpe.api.client.oidc.OidcClient delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public Configuration getConfiguration() throws OidcClientException
	{
		var configuration = delegate.getConfiguration();
		return configuration == null ? null : new ConfigurationApiDelegate(configuration);
	}

	@Override
	public Jwks getJwks() throws OidcClientException
	{
		var jwks = delegate.getJwks();
		return jwks == null ? null : new JwksApiDelegate(jwks);
	}

	@Override
	public char[] getAccessToken() throws OidcClientException
	{
		return delegate.getAccessToken();
	}

	@Override
	public char[] getAccessToken(Configuration configuration, Jwks jwks) throws OidcClientException
	{
		return delegate.getAccessToken(configuration == null ? null : new ConfigurationV2Delegate(configuration),
				jwks == null ? null : new JwksV2Delegate(jwks));
	}
}
