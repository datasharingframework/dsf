package dev.dsf.bpe.client;

public interface FhirWebserviceClient extends BasicFhirWebserviceClient, RetryClient<BasicFhirWebserviceClient>
{
	PreferReturnMinimalWithRetry withMinimalReturn();
}
