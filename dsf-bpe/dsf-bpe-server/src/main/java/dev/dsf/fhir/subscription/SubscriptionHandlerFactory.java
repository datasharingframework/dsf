package dev.dsf.fhir.subscription;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.client.FhirWebserviceClient;

public interface SubscriptionHandlerFactory<R extends Resource>
{
	ExistingResourceLoader<R> createExistingResourceLoader(FhirWebserviceClient client);

	EventResourceHandler<R> createEventResourceHandler();

	PingEventResourceHandler<R> createPingEventResourceHandler(ExistingResourceLoader<R> existingResourceLoader);
}
