package dev.dsf.fhir.authorization.process;

import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.Role;

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
	public String getOrganizationIdentifierValue()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Role> getRoles()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasRole(Role role)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasRole(String role)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<X509Certificate> getCertificate()
	{
		throw new UnsupportedOperationException();
	}
}
