package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Optional;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;

public interface EndpointProvider
{
	String ENDPOINT_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/endpoint-identifier";

	Optional<Endpoint> getLocalEndpoint();

	Optional<String> getLocalEndpointIdentifierValue();

	Optional<Endpoint> getEndpoint(Organization organization, X509Certificate x509Certificate);
}
