package dev.dsf.common.jetty;

import java.security.KeyStore;
import java.time.Duration;

import org.eclipse.jetty.client.ProxyConfiguration.Proxy;

public record OidcConfig(boolean authorizationCodeFlowEnabled, boolean bearerTokenEnabled, String providerBaseUrl,
		String clientId, String clientSecret, boolean bckChannelLogoutEnabled, String ssoBackChannelPath,
		Duration clientIdleTimeout, Duration clientConnectTimeout, KeyStore clientTrustStore, KeyStore clientKeyStore,
		char[] clientKeyStorePassword, Proxy clientProxy)
{
}
