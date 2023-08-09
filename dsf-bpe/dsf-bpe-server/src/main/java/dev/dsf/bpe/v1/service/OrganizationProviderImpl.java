package dev.dsf.bpe.v1.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrganizationProviderImpl extends AbstractResourceProvider implements OrganizationProvider
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationProviderImpl.class);

	public OrganizationProviderImpl(FhirWebserviceClientProvider clientProvider, String localEndpointAddress)
	{
		super(clientProvider, localEndpointAddress);
	}

	@Override
	public Optional<Organization> getLocalOrganization()
	{
		Bundle resultBundle = clientProvider.getLocalWebserviceClient().searchWithStrictHandling(Endpoint.class,
				Map.of("status", Collections.singletonList("active"), "address",
						Collections.singletonList(localEndpointAddress), "_include",
						Collections.singletonList("Endpoint:organization")));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getEntry().size() != 2
				|| resultBundle.getEntry().get(0).getResource() == null
				|| !(resultBundle.getEntry().get(0).getResource() instanceof Endpoint)
				|| resultBundle.getEntry().get(1).getResource() == null
				|| !(resultBundle.getEntry().get(1).getResource() instanceof Organization))
		{
			logger.warn("No active (or more than one) Endpoint found for address '{}'", localEndpointAddress);
			return Optional.empty();
		}
		else if (getActiveOrganizationFromIncludes(resultBundle).count() != 1)
		{
			logger.warn("No active (or more than one) Organization found by active Endpoint with address '{}'",
					localEndpointAddress);
			return Optional.empty();
		}

		return getActiveOrganizationFromIncludes(resultBundle).findFirst();
	}

	private Stream<Organization> getActiveOrganizationFromIncludes(Bundle resultBundle)
	{
		return resultBundle.getEntry().stream().filter(BundleEntryComponent::hasSearch)
				.filter(e -> SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(r -> r instanceof Organization).map(r -> (Organization) r).filter(Organization::getActive);
	}

	@Override
	public Optional<Organization> getOrganization(Identifier organizationIdentifier)
	{
		if (organizationIdentifier == null)
		{
			logger.debug("Organization identifier is null");
			return Optional.empty();
		}

		String organizationIdSp = toSearchParameter(organizationIdentifier);

		Bundle resultBundle = clientProvider.getLocalWebserviceClient().searchWithStrictHandling(Organization.class,
				Map.of("status", Collections.singletonList("active"), "identifier",
						Collections.singletonList(organizationIdSp)));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getTotal() != 1
				|| resultBundle.getEntryFirstRep().getResource() == null
				|| !(resultBundle.getEntryFirstRep().getResource() instanceof Organization))
		{
			logger.warn("No active (or more than one) Organization found for identifier '{}'", organizationIdSp);
			return Optional.empty();
		}

		return Optional.of((Organization) resultBundle.getEntryFirstRep().getResource());
	}

	@Override
	public List<Organization> getOrganizations(Identifier parentOrganizationIdentifier)
	{
		if (parentOrganizationIdentifier == null)
		{
			logger.debug("Parent organiztion identifier is null");
			return Collections.emptyList();
		}

		String parentOrganizationIdSp = toSearchParameter(parentOrganizationIdentifier);

		Map<String, List<String>> parameters = Map.of("active", Collections.singletonList("true"),
				"primary-organization:identifier", Collections.singletonList(parentOrganizationIdSp), "_include",
				Collections.singletonList("OrganizationAffiliation:participating-organization"));

		return search(OrganizationAffiliation.class, parameters, SearchEntryMode.INCLUDE, Organization.class,
				Organization::getActive);
	}

	@Override
	public List<Organization> getOrganizations(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole)
	{
		if (parentOrganizationIdentifier == null)
		{
			logger.debug("Parent organiztion identifier is null");
			return Collections.emptyList();
		}
		else if (memberOrganizationRole == null)
		{
			logger.debug("Member organiztion role is null");
			return Collections.emptyList();
		}

		String parentOrganizationIdSp = toSearchParameter(parentOrganizationIdentifier);
		String memberOrganizationRoleSp = toSearchParameter(memberOrganizationRole);

		Map<String, List<String>> parameters = Map.of("active", Collections.singletonList("true"),
				"primary-organization:identifier", Collections.singletonList(parentOrganizationIdSp), "role",
				Collections.singletonList(memberOrganizationRoleSp), "_include",
				Collections.singletonList("OrganizationAffiliation:participating-organization"));

		return search(OrganizationAffiliation.class, parameters, SearchEntryMode.INCLUDE, Organization.class,
				Organization::getActive);
	}

	@Override
	public List<Organization> getRemoteOrganizations()
	{
		Optional<Identifier> localOrganizationIdentifier = getLocalOrganizationIdentifier();

		if (localOrganizationIdentifier.isEmpty())
		{
			logger.debug("Local organiztion identifier unknown");
			return Collections.emptyList();
		}

		Map<String, List<String>> searchParameters = Map.of("active", Collections.singletonList("true"),
				"identifier:not", Collections.singletonList(toSearchParameter(localOrganizationIdentifier.get())));
		return search(Organization.class, searchParameters, SearchEntryMode.MATCH, Organization.class, o -> true);
	}
}
