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

import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.OrganizationIdentity;

public class TestOrganizationIdentity implements OrganizationIdentity
{
	private final boolean localIdentity;
	private final Organization organization;

	private TestOrganizationIdentity(boolean localIdentity, Organization organization)
	{
		this.localIdentity = localIdentity;
		this.organization = organization;
	}

	public static TestOrganizationIdentity remote(Organization organization)
	{
		return new TestOrganizationIdentity(false, organization);
	}

	public static TestOrganizationIdentity local(Organization organization)

	{
		return new TestOrganizationIdentity(true, organization);
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
	public Optional<X509Certificate> getCertificate()
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
}
