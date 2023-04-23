package dev.dsf.bpe.client;

import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.client.WebsocketClient;

public interface FhirClientProvider
{
	FhirWebserviceClient getLocalWebserviceClient();

	FhirWebserviceClient getWebserviceClient(String webserviceUrl);
	
	WebsocketClient getLocalWebsocketClient(Runnable reconnector, String subscriptionId);

	void disconnectAll();
}
