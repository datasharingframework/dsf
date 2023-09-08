package dev.dsf.bpe.subscription;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.client.FhirWebserviceClient;

public class ConcurrentSubscriptionHandlerFactory<R extends Resource>
		implements SubscriptionHandlerFactory<R>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ConcurrentSubscriptionHandlerFactory.class);

	private final SubscriptionHandlerFactory<R> delegate;

	private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
	private final ThreadPoolExecutor executor;

	/**
	 * Uses {@link Runtime#availableProcessors()} for <b>poolThreadCount</b> if the given value is <code>&lt;= 0</code>.
	 *
	 * @param poolThreadCount
	 * @param delegate
	 *            not <code>null</code>
	 */
	public ConcurrentSubscriptionHandlerFactory(int poolThreadCount, SubscriptionHandlerFactory<R> delegate)
	{
		if (poolThreadCount <= 0)
			poolThreadCount = Runtime.getRuntime().availableProcessors();

		executor = new ThreadPoolExecutor(poolThreadCount, poolThreadCount, 10, TimeUnit.MINUTES, queue,
				(r, executor) -> logger.error("Unable to handle Task - execution rejected"));

		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public ExistingResourceLoader<R> createExistingResourceLoader(FhirWebserviceClient client)
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
