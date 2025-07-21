package dev.dsf.common.auth.conf;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;

public interface Identity extends Principal
{
	String ORGANIZATION_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/organization-identifier";
	String ENDPOINT_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/endpoint-identifier";

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

	/**
	 * @return {@link Optional#empty()} if more no {@link Endpoint} matches the external users thumbprint or more then
	 *         one {@link Endpoint} configured for the external users organization
	 */
	Optional<Endpoint> getEndpoint();

	Optional<String> getEndpointIdentifierValue();
}
