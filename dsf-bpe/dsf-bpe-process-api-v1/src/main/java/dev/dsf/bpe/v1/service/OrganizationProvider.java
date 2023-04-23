package dev.dsf.bpe.v1.service;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;

public interface OrganizationProvider
{
	Optional<Organization> getLocalOrganization();

	default Optional<Identifier> getLocalOrganizationIdentifier()
	{
		return OrganizationIdentifier.findFirst(getLocalOrganization());
	}

	default Optional<String> getLocalOrganizationIdentifierValue()
	{
		return getLocalOrganizationIdentifier().map(Identifier::getValue);
	}

	Optional<Organization> getOrganization(Identifier organizationIdentifier);

	default Optional<Organization> getOrganization(String organizationIdentifierValue)
	{
		return getOrganization(OrganizationIdentifier.withValue(organizationIdentifierValue));
	}

	List<Organization> getOrganizations(Identifier parentOrganizationIdentifier);

	default List<Organization> getOrganizations(String parentOrganizationIdentifierValue)
	{
		return getOrganizations(OrganizationIdentifier.withValue(parentOrganizationIdentifierValue));
	}

	List<Organization> getOrganizations(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole);

	default List<Organization> getOrganizations(String parentOrganizationIdentifierValue, Coding memberOrganizationRole)
	{
		return getOrganizations(OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				memberOrganizationRole);
	}

	List<Organization> getRemoteOrganizations();
}
