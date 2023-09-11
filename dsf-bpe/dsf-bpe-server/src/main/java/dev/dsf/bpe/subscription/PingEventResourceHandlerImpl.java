package dev.dsf.bpe.subscription;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingEventResourceHandlerImpl<R extends Resource> implements PingEventResourceHandler<R>
{
	private static final Logger logger = LoggerFactory.getLogger(PingEventResourceHandlerImpl.class);

	private final ExistingResourceLoader<R> loader;

	public PingEventResourceHandlerImpl(ExistingResourceLoader<R> loader)
	{
		this.loader = loader;
	}

	@Override
	public void onPing(String ping, String subscriptionIdPart, Map<String, List<String>> searchCriteriaQueryParameters)
	{
		logger.trace("Ping for subscription {} received", ping);
		if (!subscriptionIdPart.equals(ping))
		{
			logger.warn("Received ping for subscription {}, but expected subscription {}, ignoring ping", ping,
					subscriptionIdPart);
			return;
		}

		loader.readExistingResources(searchCriteriaQueryParameters);
	}
}
