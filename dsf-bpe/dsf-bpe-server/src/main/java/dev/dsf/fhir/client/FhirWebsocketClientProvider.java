package dev.dsf.fhir.client;

public interface FhirWebsocketClientProvider extends FhirWebserviceClientProvider
{
	WebsocketClient getLocalWebsocketClient(Runnable reconnector, String subscriptionId);

	void disconnectAll();
}
