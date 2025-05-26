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
