package dev.dsf.bpe.v1.service;

import dev.dsf.fhir.client.FhirWebserviceClient;

public interface FhirWebserviceClientProvider
{
	FhirWebserviceClient getLocalWebserviceClient();

	FhirWebserviceClient getWebserviceClient(String webserviceUrl);
}