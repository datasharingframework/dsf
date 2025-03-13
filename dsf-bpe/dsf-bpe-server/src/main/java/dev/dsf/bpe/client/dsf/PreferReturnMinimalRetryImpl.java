package dev.dsf.bpe.client.dsf;

import org.hl7.fhir.r4.model.Bundle;

class PreferReturnMinimalRetryImpl extends AbstractWebserviceClientJerseyWithRetry implements PreferReturnMinimal
{
	PreferReturnMinimalRetryImpl(WebserviceClientJersey delegate, int nTimes, long delayMillis)
	{
		super(delegate, nTimes, delayMillis);
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(nTimes, delayMillis, () -> delegate.postBundle(PreferReturnType.MINIMAL, bundle));
	}
}