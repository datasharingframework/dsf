package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Optional;

import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.hl7.fhir.r4.model.Practitioner;

public interface PractitionerProvider
{
	String PRACTITIONER_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/practitioner-identifier";

	Optional<Practitioner> getPractitioner(OpenIdCredentials credentials);

	Optional<Practitioner> getPractitioner(X509Certificate certificate);
}
