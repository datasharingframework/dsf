/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.v2.service;

import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Endpoint;

import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.constants.NamingSystems;

/**
 * Provides DSF clients for configured (non DSF) FHIR servers and DSF FHIR servers.
 *
 * @see FhirClientProvider
 * @see ClientConfigProvider
 */
public interface DsfClientProvider
{
	/**
	 * DSF client for a FHIR server configured via YAML with the given <b>fhirServerId</b>.<br>
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
	 */
	Optional<DsfClient> getById(String fhirServerId);

	DsfClient getLocal();

	/**
	 * @param webserviceUrl
	 *            not <code>null</code>
	 * @return {@link DsfClient} for the given <b>webserviceUrl</b>
	 */
	DsfClient getByEndpointUrl(String webserviceUrl);

	/**
	 * @param endpoint
	 *            not <code>null</code>, endpoint.address not <code>null</code>
	 * @return {@link DsfClient} for the address defined in the given <b>endpoint</b>
	 */
	default DsfClient getByEndpoint(Endpoint endpoint)
	{
		Objects.requireNonNull(endpoint, "endpoint");

		return getByEndpointUrl(endpoint.getAddress());
	}
}