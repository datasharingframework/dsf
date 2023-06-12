package dev.dsf.common.auth;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.security.openid.OpenIdConfiguration;
import org.eclipse.jetty.util.component.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.interfaces.RSAKeyProvider;

import dev.dsf.common.auth.jwk.Jwks;

public class DsfOpenIdConfiguration extends OpenIdConfiguration
{
	private static final Logger logger = LoggerFactory.getLogger(DsfOpenIdConfiguration.class);

	private final boolean backChannelLogoutEnabled;
	private final boolean bearerTokenEnabled;

	private RSAKeyProvider rsaKeyProvider;

	public DsfOpenIdConfiguration(String issuer, String clientId, String clientSecret, HttpClient httpClient,
			boolean backChannelLogoutEnabled, boolean bearerTokenEnabled)
	{
		super(issuer, null, null, clientId, clientSecret, httpClient);

		this.backChannelLogoutEnabled = backChannelLogoutEnabled;
		this.bearerTokenEnabled = bearerTokenEnabled;
	}

	@Override
	protected void processMetadata(Map<String, Object> discoveryDocument)
	{
		super.processMetadata(discoveryDocument);

		if (backChannelLogoutEnabled || bearerTokenEnabled)
		{
			String jwksUri = (String) discoveryDocument.get("jwks_uri");
			if (jwksUri == null)
				throw new IllegalStateException("jwks_uri");

			this.rsaKeyProvider = createRsaKeyProvider(jwksUri);
		}
	}

	public boolean isBackChannelLogoutEnabled()
	{
		return backChannelLogoutEnabled;
	}

	/**
	 * @return <code>null</code> if {@link #isBackChannelLogoutEnabled()} returns false, or this ({@link LifeCycle})
	 *         object was not initialized
	 * @see #start()
	 */
	public RSAKeyProvider getRsaKeyProvider()
	{
		return rsaKeyProvider;
	}

	private RSAKeyProvider createRsaKeyProvider(String jwksUri)
	{
		try
		{
			Jwks jwks = Jwks.from(getHttpClient().GET(jwksUri).getContentAsString());
			return new RSAKeyProvider()
			{
				@Override
				public RSAPublicKey getPublicKeyById(String kid)
				{
					Optional<RSAPublicKey> key = jwks.getKey(kid).getPublicKey();
					if (key.isPresent())
						return key.get();
					else
					{
						logger.warn("Unable to retrieve key with id " + kid);
						return null;
					}
				}

				@Override
				public RSAPrivateKey getPrivateKey()
				{
					return null;
				}

				@Override
				public String getPrivateKeyId()
				{
					return null;
				}
			};
		}
		catch (InterruptedException | ExecutionException | TimeoutException e)
		{
			logger.warn("Unable to retrieve keys from {}: {} - {}", jwksUri, e.getClass().getName(), e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
