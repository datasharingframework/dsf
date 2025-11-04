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

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;

public abstract class AbstractIdentity implements Identity
{
	private final boolean localIdentity;
	private final Organization organization;
	private final Endpoint endpoint;
	private final Set<DsfRole> dsfRoles = new HashSet<>();
	private final X509Certificate certificate;

	/**
	 * @param localIdentity
	 *            <code>true</code> if this is a local identity
	 * @param organization
	 *            not <code>null</code>
	 * @param endpoint
	 *            may be <code>null</code>
	 * @param dsfRoles
	 *            may be <code>null</code>
	 * @param certificate
	 *            may be <code>null</code>
	 */
	public AbstractIdentity(boolean localIdentity, Organization organization, Endpoint endpoint,
			Collection<? extends DsfRole> dsfRoles, X509Certificate certificate)
	{
		this.localIdentity = localIdentity;
		this.organization = Objects.requireNonNull(organization, "organization");
		this.endpoint = endpoint;

		if (dsfRoles != null)
			this.dsfRoles.addAll(dsfRoles);

		this.certificate = certificate;
	}

	@Override
	public boolean isLocalIdentity()
	{
		return localIdentity;
	}

	@Override
	public Organization getOrganization()
	{
		return organization;
	}

	@Override
	public Optional<String> getOrganizationIdentifierValue()
	{
		return getIdentifierValue(organization::getIdentifier, ORGANIZATION_IDENTIFIER_SYSTEM);
	}

	protected Optional<String> getIdentifierValue(Supplier<List<Identifier>> identifiers, String identifierSystem)
	{
		Objects.requireNonNull(identifiers, "identifiers");
		Objects.requireNonNull(identifierSystem, "identifierSystem");

		List<Identifier> ids = identifiers.get();
		if (ids == null)
			return Optional.empty();

		return ids.stream().filter(i -> i != null).filter(i -> identifierSystem.equals(i.getSystem()))
				.filter(Identifier::hasValue).findFirst().map(Identifier::getValue);
	}

	@Override
	public Set<DsfRole> getDsfRoles()
	{
		return Collections.unmodifiableSet(dsfRoles);
	}

	@Override
	public boolean hasDsfRole(DsfRole dsfRole)
	{
		return dsfRoles.stream().anyMatch(r -> r.matches(dsfRole));
	}

	@Override
	public Optional<X509Certificate> getCertificate()
	{
		// null if login via OIDC
		return Optional.ofNullable(certificate);
	}

	@Override
	public Optional<Endpoint> getEndpoint()
	{
		return Optional.ofNullable(endpoint);
	}

	@Override
	public Optional<String> getEndpointIdentifierValue()
	{
		return getEndpoint().flatMap(e -> getIdentifierValue(e::getIdentifier, ENDPOINT_IDENTIFIER_SYSTEM));
	}
}
