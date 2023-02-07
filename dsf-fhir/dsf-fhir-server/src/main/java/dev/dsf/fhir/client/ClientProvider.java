package dev.dsf.fhir.client;

import java.util.Optional;

public interface ClientProvider
{
	Optional<FhirWebserviceClient> getClient(String serverBase);

	boolean endpointExists(String serverBase);
}
