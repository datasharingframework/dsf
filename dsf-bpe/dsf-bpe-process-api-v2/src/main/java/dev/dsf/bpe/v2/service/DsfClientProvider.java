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