package dev.dsf.bpe.api.config;

import java.security.KeyStore;

public interface ClientConfig
{
	String getFhirServerBaseUrl();

	KeyStore getWebserviceKeyStore(char[] keyStorePassword);

	KeyStore getWebserviceTrustStore();

	int getWebserviceClientLocalReadTimeout();

	int getWebserviceClientLocalConnectTimeout();

	boolean getWebserviceClientLocalVerbose();

	int getWebserviceClientRemoteReadTimeout();

	int getWebserviceClientRemoteConnectTimeout();

	boolean getWebserviceClientRemoteVerbose();
}
