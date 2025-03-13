package dev.dsf.bpe.subscription;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.bpe.client.dsf.WebserviceClient;

public interface SubscriptionHandlerFactory<R extends Resource>
{
	ExistingResourceLoader<R> createExistingResourceLoader(WebserviceClient client);

	EventResourceHandler<R> createEventResourceHandler();

	PingEventResourceHandler<R> createPingEventResourceHandler(ExistingResourceLoader<R> existingResourceLoader);
}
