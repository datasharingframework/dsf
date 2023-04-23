package dev.dsf.bpe.v1.service;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;

import dev.dsf.bpe.v1.constants.NamingSystems.EndpointIdentifier;
import dev.dsf.bpe.v1.constants.NamingSystems.OrganizationIdentifier;

public interface EndpointProvider
{
	String getLocalEndpointAddress();

	Optional<Endpoint> getLocalEndpoint();

	default Optional<Identifier> getLocalEndpointIdentifier()
	{
		return EndpointIdentifier.findFirst(getLocalEndpoint());
	}

	default Optional<String> getLocalEndpointIdentifierValue()
	{
		return getLocalEndpointIdentifier().map(Identifier::getValue);
	}

	Optional<Endpoint> getEndpoint(Identifier endpointIdentifier);

	default Optional<Endpoint> getEndpoint(String endpointIdentifierValue)
	{
		return getEndpoint(EndpointIdentifier.withValue(endpointIdentifierValue));
	}

	default Optional<String> getEndpointAddress(Identifier endpointIdentifier)
	{
		return getEndpoint(endpointIdentifier).map(Endpoint::getAddress);
	}

	default Optional<String> getEndpointAddress(String endpointIdentifierValue)
	{
		return getEndpointAddress(EndpointIdentifier.withValue(endpointIdentifierValue));
	}

	Optional<Endpoint> getEndpoint(Identifier parentOrganizationIdentifier, Identifier memberOrganizationIdentifier,
			Coding memberOrganizationRole);

	default Optional<Endpoint> getEndpoint(String parentOrganizationIdentifierValue,
			String memberOrganizationIdentifierValue, Coding memberOrganizationRole)
	{
		return getEndpoint(OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				OrganizationIdentifier.withValue(memberOrganizationIdentifierValue), memberOrganizationRole);
	}

	default Optional<String> getEndpointAddress(Identifier parentOrganizationIdentifierValue,
			Identifier memberOrganizationIdentifierValue, Coding memberOrganizationRole)
	{
		return getEndpoint(parentOrganizationIdentifierValue, memberOrganizationIdentifierValue, memberOrganizationRole)
				.map(Endpoint::getAddress);
	}

	default Optional<String> getEndpointAddress(String parentOrganizationIdentifierValue,
			String memberOrganizationIdentifierValue, Coding memberOrganizationRole)
	{
		return getEndpointAddress(OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				OrganizationIdentifier.withValue(memberOrganizationIdentifierValue), memberOrganizationRole);
	}

	List<Endpoint> getEndpoints(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole);

	default List<Endpoint> getEndpoints(String parentOrganizationIdentifierValue, Coding memberOrganizationRole)
	{
		return getEndpoints(OrganizationIdentifier.withValue(parentOrganizationIdentifierValue),
				memberOrganizationRole);
	}
}
