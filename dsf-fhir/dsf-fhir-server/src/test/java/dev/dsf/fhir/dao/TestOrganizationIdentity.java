package dev.dsf.fhir.dao;

import java.util.Set;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.OrganizationIdentityImpl;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;

public class TestOrganizationIdentity extends OrganizationIdentityImpl
{
	private TestOrganizationIdentity(boolean localIdentity, Organization organization, Set<? extends DsfRole> roles)
	{
		super(localIdentity, organization, null, roles, null);
	}

	public static TestOrganizationIdentity local(Organization organization)
	{
		return new TestOrganizationIdentity(true, organization, FhirServerRoleImpl.LOCAL_ORGANIZATION);
	}

	public static TestOrganizationIdentity remote(Organization organization)
	{
		return new TestOrganizationIdentity(false, organization, FhirServerRoleImpl.REMOTE_ORGANIZATION);
	}
}
