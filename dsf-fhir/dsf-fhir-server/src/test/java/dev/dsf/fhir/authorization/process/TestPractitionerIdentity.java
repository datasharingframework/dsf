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
package dev.dsf.fhir.authorization.process;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.DsfOpenIdCredentials;
import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.common.auth.conf.X509CertificateWrapper;

public class TestPractitionerIdentity implements PractitionerIdentity
{
	private final Organization organization;
	private final Set<Coding> roles = new HashSet<>();

	private TestPractitionerIdentity(Organization organization, Collection<Coding> roles)
	{
		this.organization = organization;

		if (roles != null)
			this.roles.addAll(roles);
	}

	public static TestPractitionerIdentity practitioner(Organization organization, Coding... roles)
	{
		return new TestPractitionerIdentity(organization, List.of(roles));
	}

	@Override
	public String getName()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public String getDisplayName()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isLocalIdentity()
	{
		return true;
	}

	@Override
	public Organization getOrganization()
	{
		return organization;
	}

	@Override
	public Optional<String> getOrganizationIdentifierValue()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<DsfRole> getDsfRoles()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasDsfRole(DsfRole role)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<X509CertificateWrapper> getCertificate()
	{
		throw new UnsupportedOperationException();

	}

	@Override
	public Practitioner getPractitioner()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Coding> getPractionerRoles()
	{
		return roles;
	}

	@Override
	public Optional<DsfOpenIdCredentials> getCredentials()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<Endpoint> getEndpoint()
	{
		return Optional.empty();
	}

	@Override
	public Optional<String> getEndpointIdentifierValue()
	{
		return Optional.empty();
	}

	@Override
	public Optional<String> getPractitionerIdentifierValue()
	{
		return Optional.empty();
	}
}
