package dev.dsf.fhir.dao;

import java.util.Set;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.Role;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.OrganizationIdentityImpl;

public class TestOrganizationIdentity extends OrganizationIdentityImpl
{
	private TestOrganizationIdentity(boolean localIdentity, Organization organization, Set<? extends Role> roles)
	{
		super(localIdentity, organization, roles);
	}

	public static TestOrganizationIdentity local(Organization organization)
	{
		return new TestOrganizationIdentity(true, organization, FhirServerRole.LOCAL_ORGANIZATION);
	}

	public static TestOrganizationIdentity remote(Organization organization)
	{
		return new TestOrganizationIdentity(false, organization, FhirServerRole.REMOTE_ORGANIZATION);
	}
}
