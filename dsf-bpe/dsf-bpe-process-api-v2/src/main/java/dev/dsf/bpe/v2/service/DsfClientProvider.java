package dev.dsf.bpe.v2.service;

import java.util.Objects;

import org.hl7.fhir.r4.model.Endpoint;

import dev.dsf.bpe.v2.client.dsf.DsfClient;

/**
 * Provides clients for DSF FHIR servers.
 *
 * @see FhirClientProvider
 */
public interface DsfClientProvider
{
	DsfClient getLocalDsfClient();

	/**
	 * @param webserviceUrl
	 *            not <code>null</code>
	 * @return {@link DsfClient} for the given <b>webserviceUrl</b>
	 */
	DsfClient getDsfClient(String webserviceUrl);

	/**
	 * @param endpoint
	 *            not <code>null</code>, endpoint.address not <code>null</code>
	 * @return {@link DsfClient} for the address defined in the given <b>endpoint</b>
	 */
	default DsfClient getDsfClient(Endpoint endpoint)
	{
		Objects.requireNonNull(endpoint, "endpoint");

		return getDsfClient(endpoint.getAddress());
	}
}