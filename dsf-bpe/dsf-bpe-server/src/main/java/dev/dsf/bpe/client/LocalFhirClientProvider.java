package dev.dsf.bpe.client;

import dev.dsf.fhir.client.WebsocketClient;

public interface LocalFhirClientProvider
{
	FhirWebserviceClient getLocalWebserviceClient();

	WebsocketClient getLocalWebsocketClient(Runnable reconnector, String subscriptionId);

	void disconnectAll();
}
