package dev.dsf.bpe.client.dsf;

import java.security.KeyStore;

import dev.dsf.fhir.client.WebsocketClient;

public interface ClientProvider
{
	WebserviceClient getWebserviceClient();

	WebsocketClient getWebsocketClient(Runnable reconnector, String subscriptionId);

	void disconnectAll();

	char[] getWebserviceKeyStorePassword();

	KeyStore getWebserviceKeyStore();

	KeyStore getWebserviceTrustStore();
}
