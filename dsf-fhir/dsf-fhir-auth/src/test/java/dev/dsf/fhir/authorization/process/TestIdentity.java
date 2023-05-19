package dev.dsf.fhir.authorization.process;

import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.Identity;

public class TestIdentity implements Identity
{
	private final boolean localIdentity;
	private final Organization organization;

	private TestIdentity(boolean localIdentity, Organization organization)
	{
		this.localIdentity = localIdentity;
		this.organization = organization;
	}

	public static TestIdentity remote(Organization organization)
	{
		return new TestIdentity(false, organization);
	}

	public static TestIdentity local(Organization organization)

	{
		return new TestIdentity(true, organization);
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
}
