package dev.dsf.bpe.client.dsf;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

class BasicWebserviceCientWithRetryImpl extends AbstractWebserviceClientJerseyWithRetry implements BasicWebserviceClient
{
	BasicWebserviceCientWithRetryImpl(WebserviceClientJersey delegate, int nTimes, Duration delayMillis)
	{
		super(delegate, nTimes, delayMillis);
	}

	@Override
	public <R extends Resource> R update(R resource)
	{
		return retry(() -> delegate.update(resource));
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(() -> delegate.postBundle(bundle));
	}

	@Override
	public Bundle searchWithStrictHandling(Class<? extends Resource> resourceType, Map<String, List<String>> parameters)
	{
		return retry(() -> delegate.searchWithStrictHandling(resourceType, parameters));
	}
}