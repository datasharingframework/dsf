package dev.dsf.common.auth;

import java.security.Principal;
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

	String getOrganizationIdentifierValue();

	Set<Role> getRoles();

	boolean hasRole(Role role);

	boolean hasRole(String role);
}
