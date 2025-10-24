package dev.dsf.bpe.client.dsf;

import java.time.Duration;

import org.hl7.fhir.r4.model.Bundle;

class PreferReturnMinimalRetryImpl extends AbstractWebserviceClientJerseyWithRetry implements PreferReturnMinimal
{
	PreferReturnMinimalRetryImpl(WebserviceClientJersey delegate, int nTimes, Duration delay)
	{
		super(delegate, nTimes, delay);
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(() -> delegate.postBundle(PreferReturnType.MINIMAL, bundle));
	}
}