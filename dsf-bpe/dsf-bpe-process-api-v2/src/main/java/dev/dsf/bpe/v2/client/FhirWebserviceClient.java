package dev.dsf.bpe.v2.client;

public interface FhirWebserviceClient extends BasicFhirWebserviceClient, RetryClient<BasicFhirWebserviceClient>
{
	String getBaseUrl();

	PreferReturnOutcomeWithRetry withOperationOutcomeReturn();

	PreferReturnMinimalWithRetry withMinimalReturn();
}
