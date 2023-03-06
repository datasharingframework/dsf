package dev.dsf.common.auth;

import org.hl7.fhir.r4.model.Practitioner;

public interface PractitionerIdentity extends Identity
{
	String PRACTITIONER_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/practitioner-identifier";

	/**
	 * @return never <code>null</code>
	 */
	Practitioner getPractitioner();
}
