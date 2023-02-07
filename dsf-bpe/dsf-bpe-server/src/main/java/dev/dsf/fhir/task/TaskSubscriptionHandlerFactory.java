package dev.dsf.fhir.task;

import java.util.Objects;

import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.dao.LastEventTimeDao;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.subscription.EventResourceHandler;
import dev.dsf.fhir.subscription.EventResourceHandlerImpl;
import dev.dsf.fhir.subscription.ExistingResourceLoader;
import dev.dsf.fhir.subscription.ExistingResourceLoaderImpl;
import dev.dsf.fhir.subscription.PingEventResourceHandler;
import dev.dsf.fhir.subscription.SubscriptionHandlerFactory;
import dev.dsf.fhir.websocket.ResourceHandler;

public class TaskSubscriptionHandlerFactory implements SubscriptionHandlerFactory<Task>, InitializingBean
{
	private final ResourceHandler<Task> resourceHandler;
	private final LastEventTimeDao lastEventTimeDao;

	public TaskSubscriptionHandlerFactory(ResourceHandler<Task> resourceHandler, LastEventTimeDao lastEventTimeDao)
	{
		this.resourceHandler = resourceHandler;
		this.lastEventTimeDao = lastEventTimeDao;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(resourceHandler, "resourceHandler");
		Objects.requireNonNull(lastEventTimeDao, "lastEventTimeDao");
	}

	@Override
	public ExistingResourceLoader<Task> createExistingResourceLoader(FhirWebserviceClient client)
	{
		return new ExistingResourceLoaderImpl<>(lastEventTimeDao, resourceHandler, client, "Task", Task.class);
	}

	@Override
	public EventResourceHandler<Task> createEventResourceHandler()
	{
		return new EventResourceHandlerImpl<>(lastEventTimeDao, resourceHandler, Task.class);
	}

	@Override
	public PingEventResourceHandler<Task> createPingEventResourceHandler(
			ExistingResourceLoader<Task> existingResourceLoader)
	{
		return new PingEventResourceHandler<>(existingResourceLoader);
	}
}
