package dev.dsf.bpe.service;

import java.util.Optional;

import org.hl7.fhir.r4.model.Organization;

public interface LocalOrganizationProvider
{
	Optional<Organization> getLocalOrganization();
}
