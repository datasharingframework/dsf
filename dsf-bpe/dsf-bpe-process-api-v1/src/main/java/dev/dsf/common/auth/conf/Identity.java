package dev.dsf.common.auth.conf;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Organization;

public interface Identity extends Principal
{
	String ORGANIZATION_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/organization-identifier";

	boolean isLocalIdentity();

	/**
	 * @return never <code>null</code>
	 */
	Organization getOrganization();

	Optional<String> getOrganizationIdentifierValue();

	Set<DsfRole> getDsfRoles();

	boolean hasDsfRole(DsfRole role);

	/**
	 * @return {@link Optional#empty()} if login via OIDC
	 */
	Optional<X509Certificate> getCertificate();

	String getDisplayName();
}
