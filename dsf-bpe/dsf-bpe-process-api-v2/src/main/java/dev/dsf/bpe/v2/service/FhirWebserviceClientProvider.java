package dev.dsf.bpe.v2.service;

import dev.dsf.bpe.v2.client.FhirWebserviceClient;

public interface FhirWebserviceClientProvider
{
	FhirWebserviceClient getLocalWebserviceClient();

	FhirWebserviceClient getWebserviceClient(String webserviceUrl);
}