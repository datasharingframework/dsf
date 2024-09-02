package dev.dsf.bpe.client;

import org.hl7.fhir.r4.model.Bundle;

class PreferReturnMinimalWithRetryImpl implements PreferReturnMinimalWithRetry
{
	private final FhirWebserviceClientJersey delegate;

	PreferReturnMinimalWithRetryImpl(FhirWebserviceClientJersey delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return delegate.postBundle(PreferReturnType.MINIMAL, bundle);
	}

	@Override
	public PreferReturnMinimal withRetry(int nTimes, long delayMillis)
	{
		if (nTimes < 0)
			throw new IllegalArgumentException("nTimes < 0");
		if (delayMillis < 0)
			throw new IllegalArgumentException("delayMillis < 0");

		return new PreferReturnMinimalRetryImpl(delegate, nTimes, delayMillis);
	}

	@Override
	public PreferReturnMinimal withRetryForever(long delayMillis)
	{
		if (delayMillis < 0)
			throw new IllegalArgumentException("delayMillis < 0");

		return new PreferReturnMinimalRetryImpl(delegate, RETRY_FOREVER, delayMillis);
	}
}