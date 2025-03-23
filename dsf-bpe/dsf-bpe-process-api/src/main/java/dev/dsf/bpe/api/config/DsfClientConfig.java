package dev.dsf.bpe.api.config;

import java.security.KeyStore;
import java.time.Duration;

public interface DsfClientConfig
{
	interface BaseConfig
	{
		Duration getReadTimeout();

		Duration getConnectTimeout();

		boolean isDebugLoggingEnabled();
	}

	interface LocalConfig extends BaseConfig
	{
		String getBaseUrl();
	}

	interface RemoteConfig extends BaseConfig
	{
	}

	KeyStore getTrustStore();

	KeyStore getKeyStore();

	char[] getKeyStorePassword();

	LocalConfig getLocalConfig();

	RemoteConfig getRemoteConfig();
}
