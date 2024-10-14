package dev.dsf.fhir.client;

public interface FhirWebserviceClient extends BasicFhirWebserviceClient, RetryClient<BasicFhirWebserviceClient>
{
	String getBaseUrl();

	PreferReturnOutcomeWithRetry withOperationOutcomeReturn();

	PreferReturnMinimalWithRetry withMinimalReturn();
}
