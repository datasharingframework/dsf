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

import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.hl7.fhir.r4.model.Endpoint;

import dev.dsf.bpe.v2.client.fhir.ClientConfig;
import dev.dsf.bpe.v2.constants.NamingSystems;

/**
 * Provides connection configurations for YAML configured (non DSF) FHIR servers and DSF FHIR servers, as well as access
 * to the default certificate trust store for FHIR connections configured via the DSF BPE property
 * `dev.dsf.bpe.fhir.client.connections.config.default.trust.server.certificate.cas` as default for the YAML properties
 * `trusted-root-certificates-file` and `oidc-auth.trusted-root-certificates-file`
 */
public interface ClientConfigProvider
{
	/**
	 * <i>Every call to this method creates a new {@link SSLContext} object.</i>
	 *
	 * @return new {@link SSLContext} configured with {@link #createDefaultTrustStore()}
	 */
	SSLContext createDefaultSslContext();

	/**
	 * <i>Every call to this method creates a new {@link KeyStore} object.</i>
	 *
	 * @return copy of default certificate trust store configured via the DSF BPE config property
	 *         `dev.dsf.bpe.fhir.client.connections.config.default.trust.server.certificate.cas`
	 */
	KeyStore createDefaultTrustStore();

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
