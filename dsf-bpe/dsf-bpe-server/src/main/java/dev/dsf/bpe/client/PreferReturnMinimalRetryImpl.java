package dev.dsf.bpe.client;

import org.hl7.fhir.r4.model.Bundle;

class PreferReturnMinimalRetryImpl extends AbstractFhirWebserviceClientJerseyWithRetry implements PreferReturnMinimal
{
	PreferReturnMinimalRetryImpl(FhirWebserviceClientJersey delegate, int nTimes, long delayMillis)
	{
		super(delegate, nTimes, delayMillis);
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(nTimes, delayMillis, () -> delegate.postBundle(PreferReturnType.MINIMAL, bundle));
	}
}