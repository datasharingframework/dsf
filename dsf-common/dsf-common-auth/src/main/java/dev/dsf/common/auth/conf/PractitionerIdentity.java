package dev.dsf.common.auth.conf;

import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.DsfOpenIdCredentials;

public interface PractitionerIdentity extends Identity
{
	String PRACTITIONER_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/practitioner-identifier";

	/**
	 * @return never <code>null</code>
	 */
	Practitioner getPractitioner();

	Optional<String> getPractitionerIdentifierValue();

	/**
	 * @return never <code>null</code>
	 */
	Set<Coding> getPractionerRoles();

	/**
	 * @return {@link Optional#empty()} if login via client certificate
	 */
	Optional<DsfOpenIdCredentials> getCredentials();
}
