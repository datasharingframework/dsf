package dev.dsf.fhir.authorization.process;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.DsfOpenIdCredentials;
import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.PractitionerIdentity;

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
		return new TestPractitionerIdentity(organization, Arrays.asList(roles));
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
	public Optional<X509Certificate> getCertificate()
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
}
