package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Optional;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.common.auth.conf.Identity;

public interface OrganizationProvider
{
	String ORGANIZATION_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/organization-identifier";

	/**
	 * @param certificate
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if no {@link Organization} is found, or the given {@link X509Certificate} is
	 *         <code>null</code>
	 */
	Optional<Organization> getOrganization(X509Certificate certificate);

	Optional<Organization> getLocalOrganization();

	Optional<Identity> getLocalOrganizationAsIdentity();

	String getLocalOrganizationIdentifierValue();
}
