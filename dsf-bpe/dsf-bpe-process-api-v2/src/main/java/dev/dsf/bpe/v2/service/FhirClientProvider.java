package dev.dsf.bpe.v2.service;

import java.util.Optional;

import org.hl7.fhir.r4.model.Endpoint;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v2.client.fhir.ClientConfig;
import dev.dsf.bpe.v2.constants.NamingSystems;

/**
 * Provides connection configurations and HAPI FHIR clients for configured (non DSF) FHIR servers and DSF FHIR servers.
 *
 * @see DsfClientProvider
 */
public interface FhirClientProvider
{
	/**
	 * HAPI FHIR client for a FHIR server configured via YAML with the given <b>fhirServerId</b>.<br>
	 * <br>
	 * Use <code>#local</code> as the <b>fhirServerId</b> for a connection to the local DSF FHIR server.<br>
	 * Use <code>#&lt;value></code> as the <b>fhirServerId</b> for a connection to a DSF FHIR server with an active
	 * {@link Endpoint} resource and the given <b>fhirServerId</b> as the {@value NamingSystems.EndpointIdentifier#SID}
	 * value (ignoring the {@literal #} character).
	 *
	 * @param fhirServerId
	 *            may be <code>null</code>
	 * @return never <code>null</code>, {@link Optional#empty()} if no client is configured for the given
	 *         <b>fhirServerId</b>
	 * @see DsfClientProvider
	 */
	Optional<IGenericClient> getClient(String fhirServerId);

	/**
	 * FHIR client config for a FHIR server configured via YAML with the given <b>fhirServerId</b>.<br>
	 * <br>
	 * Use <code>#local</code> as the <b>fhirServerId</b> for a connection configuration to the local DSF FHIR
	 * server.<br>
	 * Use <code>#&lt;value></code> as the <b>fhirServerId</b> for a connection configuration to a DSF FHIR server with
	 * an active {@link Endpoint} resource and the given <b>fhirServerId</b> as the
	 * {@value NamingSystems.EndpointIdentifier#SID} value (ignoring the {@literal #} character).
	 *
	 * @param fhirServerId
	 *            may be <code>null</code>
	 * @return never <code>null</code>, {@link Optional#empty()} if no client is configured for the given
	 *         <b>fhirServerId</b>
	 * @see DsfClientProvider
	 */
	Optional<ClientConfig> getClientConfig(String fhirServerId);
}
