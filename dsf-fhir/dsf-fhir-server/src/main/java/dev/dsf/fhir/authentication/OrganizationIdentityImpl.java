package dev.dsf.fhir.authentication;

import java.util.Set;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.OrganizationIdentity;
import dev.dsf.common.auth.Role;

// TODO implement equals, hashCode, toString methods based on the DSF organization identifier to fully comply with the java.security.Principal specification
public class OrganizationIdentityImpl extends AbstractIdentity implements OrganizationIdentity
{
	/**
	 * @param localIdentity
	 *            <code>true</code> if this is a local identity
	 * @param organization
	 *            not <code>null</code>
	 * @param roles
	 *            may be <code>null</code>
	 */
	public OrganizationIdentityImpl(boolean localIdentity, Organization organization, Set<? extends Role> roles)
	{
		super(localIdentity, organization, roles);
	}

	@Override
	public String getName()
	{
		return getOrganizationIdentifierValue();
	}
}
