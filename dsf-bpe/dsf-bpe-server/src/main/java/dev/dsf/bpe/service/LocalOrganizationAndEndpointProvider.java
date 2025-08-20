package dev.dsf.bpe.service;

import java.util.Optional;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;

public interface LocalOrganizationAndEndpointProvider
{
	Optional<Organization> getLocalOrganization();

	Optional<Endpoint> getLocalEndpoint();
}
