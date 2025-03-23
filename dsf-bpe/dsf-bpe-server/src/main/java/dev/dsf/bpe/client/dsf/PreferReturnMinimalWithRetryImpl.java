package dev.dsf.bpe.client.dsf;

import java.time.Duration;

import org.hl7.fhir.r4.model.Bundle;

class PreferReturnMinimalWithRetryImpl implements PreferReturnMinimalWithRetry
{
	private final WebserviceClientJersey delegate;

	PreferReturnMinimalWithRetryImpl(WebserviceClientJersey delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return delegate.postBundle(PreferReturnType.MINIMAL, bundle);
	}

	@Override
	public PreferReturnMinimal withRetry(int nTimes, Duration delay)
	{
		if (nTimes < 0)
			throw new IllegalArgumentException("nTimes < 0");
		if (delay == null || delay.isNegative())
			throw new IllegalArgumentException("delay null or negative");

		return new PreferReturnMinimalRetryImpl(delegate, nTimes, delay);
	}

	@Override
	public PreferReturnMinimal withRetryForever(Duration delay)
	{
		if (delay == null || delay.isNegative())
			throw new IllegalArgumentException("delay null or negative");

		return new PreferReturnMinimalRetryImpl(delegate, RETRY_FOREVER, delay);
	}
}