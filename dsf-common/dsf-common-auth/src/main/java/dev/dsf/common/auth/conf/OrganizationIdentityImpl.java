package dev.dsf.common.auth.conf;

import java.security.cert.X509Certificate;
import java.util.Collection;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;

// TODO implement equals, hashCode, toString methods based on the DSF organization identifier to fully comply with the java.security.Principal specification
public class OrganizationIdentityImpl extends AbstractIdentity implements OrganizationIdentity
{
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
	public OrganizationIdentityImpl(boolean localIdentity, Organization organization, Endpoint endpoint,
			Collection<? extends DsfRole> dsfRoles, X509Certificate certificate)
	{
		super(localIdentity, organization, endpoint, dsfRoles, certificate);
	}

	@Override
	public String getName()
	{
		return getOrganizationIdentifierValue().orElse("?");
	}

	@Override
	public String getDisplayName()
	{
		return getOrganizationIdentifierValue().orElse("?");
	}
}
