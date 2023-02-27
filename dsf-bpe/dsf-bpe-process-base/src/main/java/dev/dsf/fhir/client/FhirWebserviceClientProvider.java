package dev.dsf.fhir.client;

public interface FhirWebserviceClientProvider
{
	String getLocalBaseUrl();

	FhirWebserviceClient getLocalWebserviceClient();

	FhirWebserviceClient getWebserviceClient(String webserviceUrl);
}