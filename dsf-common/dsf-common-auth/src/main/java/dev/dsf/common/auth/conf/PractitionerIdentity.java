package dev.dsf.common.auth.conf;

import java.util.Optional;

import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.hl7.fhir.r4.model.Practitioner;

public interface PractitionerIdentity extends Identity
{
	String PRACTITIONER_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/practitioner-identifier";

	/**
	 * @return never <code>null</code>
	 */
	Practitioner getPractitioner();

	/**
	 * @return {@link Optional#empty()} if login via client certificate
	 */
	Optional<OpenIdCredentials> getCredentials();
}
