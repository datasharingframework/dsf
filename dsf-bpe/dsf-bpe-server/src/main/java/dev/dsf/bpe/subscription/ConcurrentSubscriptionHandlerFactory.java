package dev.dsf.bpe.subscription;

import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.client.dsf.WebserviceClient;

public class ConcurrentSubscriptionHandlerFactory<R extends Resource>
		implements SubscriptionHandlerFactory<R>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ConcurrentSubscriptionHandlerFactory.class);

	private final SubscriptionHandlerFactory<R> delegate;
	private final ThreadPoolExecutor executor;

	/**
	 * @param corePoolSize
	 *            <code>&gt; 0</code>
	 * @param delegate
	 *            not <code>null</code>
	 */
	public ConcurrentSubscriptionHandlerFactory(int corePoolSize, SubscriptionHandlerFactory<R> delegate)
	{
		if (corePoolSize <= 0)
			throw new IllegalArgumentException("corePoolSize <= 0");

		this.delegate = delegate;

		executor = new ThreadPoolExecutor(corePoolSize, corePoolSize, 30, TimeUnit.MINUTES, new LinkedBlockingQueue<>(),
				(_, _) -> logger.error("Unable to handle Task - execution rejected"));
		executor.allowCoreThreadTimeOut(true);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public ExistingResourceLoader<R> createExistingResourceLoader(WebserviceClient client)
	{
		return delegate.createExistingResourceLoader(client);
	}

	@Override
	public EventResourceHandler<R> createEventResourceHandler()
	{
		final EventResourceHandler<R> delegateHandler = delegate.createEventResourceHandler();
		return resource -> executor.submit(() ->
		{
			logger.debug("executing onResource for {} with id: {}", resource.getResourceType().name(),
					resource.getIdElement().getValue());
			delegateHandler.onResource(resource);
		});
	}

	@Override
	public PingEventResourceHandler<R> createPingEventResourceHandler(ExistingResourceLoader<R> existingResourceLoader)
	{
		final PingEventResourceHandler<R> delegateHandler = delegate
				.createPingEventResourceHandler(existingResourceLoader);
		return (ping, subscriptionIdPart, searchCriteriaQueryParameters) -> executor
				.submit(() -> delegateHandler.onPing(ping, subscriptionIdPart, searchCriteriaQueryParameters));
	}
}
