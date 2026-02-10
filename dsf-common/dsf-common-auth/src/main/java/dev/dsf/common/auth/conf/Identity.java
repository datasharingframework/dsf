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
package dev.dsf.common.auth.conf;

import java.security.Principal;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;

public interface Identity extends Principal
{
	String ORGANIZATION_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/organization-identifier";
	String ENDPOINT_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/endpoint-identifier";

	boolean isLocalIdentity();

	/**
	 * @return never <code>null</code>
	 */
	Organization getOrganization();

	Optional<String> getOrganizationIdentifierValue();

	Set<DsfRole> getDsfRoles();

	boolean hasDsfRole(DsfRole role);

	/**
	 * @return {@link Optional#empty()} if login via OIDC
	 */
	Optional<X509CertificateWrapper> getCertificate();

	String getDisplayName();

	/**
	 * @return {@link Optional#empty()} if more no {@link Endpoint} matches the external users thumbprint or more then
	 *         one {@link Endpoint} configured for the external users organization
	 */
	Optional<Endpoint> getEndpoint();

	Optional<String> getEndpointIdentifierValue();
}
