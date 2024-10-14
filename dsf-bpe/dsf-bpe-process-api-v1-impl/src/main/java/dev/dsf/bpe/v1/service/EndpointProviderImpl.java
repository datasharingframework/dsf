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
import org.hl7.fhir.r4.model.Endpoint.EndpointStatus;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndpointProviderImpl extends AbstractResourceProvider implements EndpointProvider
{
	private static final Logger logger = LoggerFactory.getLogger(EndpointProviderImpl.class);

	public EndpointProviderImpl(FhirWebserviceClientProvider clientProvider, String localEndpointAddress)
	{
		super(clientProvider, localEndpointAddress);
	}

	@Override
	public Optional<Endpoint> getLocalEndpoint()
	{
		Bundle resultBundle = clientProvider.getLocalWebserviceClient().searchWithStrictHandling(Endpoint.class,
				Map.of("status", Collections.singletonList("active"), "address",
						Collections.singletonList(localEndpointAddress)));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getEntry().size() != 1
				|| resultBundle.getEntryFirstRep().getResource() == null
				|| !(resultBundle.getEntryFirstRep().getResource() instanceof Endpoint))
		{
			logger.warn("No active (or more than one) Endpoint found with address '{}'", localEndpointAddress);
			return Optional.empty();
		}

		return Optional.of((Endpoint) resultBundle.getEntryFirstRep().getResource());
	}

	@Override
	public String getLocalEndpointAddress()
	{
		return localEndpointAddress;
	}

	@Override
	public Optional<Endpoint> getEndpoint(Identifier endpointIdentifier)
	{
		if (endpointIdentifier == null)
		{
			logger.debug("Endpoint identifier is null");
			return Optional.empty();
		}

		String endpointIdSp = toSearchParameter(endpointIdentifier);

		Bundle resultBundle = clientProvider.getLocalWebserviceClient().searchWithStrictHandling(Endpoint.class, Map.of(
				"status", Collections.singletonList("active"), "identifier", Collections.singletonList(endpointIdSp)));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getTotal() != 1
				|| resultBundle.getEntryFirstRep().getResource() == null
				|| !(resultBundle.getEntryFirstRep().getResource() instanceof Endpoint))
		{
			logger.warn("No active (or more than one) Endpoint found with identifier '{}'", endpointIdSp);
			return Optional.empty();
		}

		return Optional.of((Endpoint) resultBundle.getEntryFirstRep().getResource());
	}

	@Override
	public Optional<Endpoint> getEndpoint(Identifier parentOrganizationIdentifier,
			Identifier memberOrganizationIdentifier, Coding memberOrganizationRole)
	{
		if (parentOrganizationIdentifier == null)
		{
			logger.debug("Parent organiztion identifier is null");
			return Optional.empty();
		}
		else if (memberOrganizationIdentifier == null)
		{
			logger.debug("Member organiztion identifier is null");
			return Optional.empty();
		}
		else if (memberOrganizationRole == null)
		{
			logger.debug("Member organiztion role is null");
			return Optional.empty();
		}

		String parentOrganizationIdSp = toSearchParameter(parentOrganizationIdentifier);
		String memberOrganizationIdSp = toSearchParameter(memberOrganizationIdentifier);
		String memberOrganizationRoleSp = toSearchParameter(memberOrganizationRole);

		Bundle resultBundle = clientProvider.getLocalWebserviceClient().searchWithStrictHandling(
				OrganizationAffiliation.class,
				Map.of("active", Collections.singletonList("true"), "primary-organization:identifier",
						Collections.singletonList(parentOrganizationIdSp), "participating-organization:identifier",
						Collections.singletonList(memberOrganizationIdSp), "role",
						Collections.singletonList(memberOrganizationRoleSp), "_include",
						Collections.singletonList("OrganizationAffiliation:endpoint")));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getTotal() != 1
				|| resultBundle.getEntryFirstRep().getResource() == null
				|| !(resultBundle.getEntryFirstRep().getResource() instanceof OrganizationAffiliation))
		{
			logger.warn(
					"No active (or more than one) OrganizationAffiliation found with primary-organization identifier '{}', participating-organization identifier '{}' and role '{}'",
					parentOrganizationIdSp, memberOrganizationIdSp, memberOrganizationRoleSp);
			return Optional.empty();
		}
		else if (getActiveEndpointFromInclude(resultBundle).count() != 1)
		{
			logger.warn(
					"No active Endpoint found for active OrganizationAffiliation with primary-organization identifier '{}', participating-organization identifier '{}' and role '{}'",
					parentOrganizationIdSp, memberOrganizationIdSp, memberOrganizationRoleSp);
			return Optional.empty();
		}

		return getActiveEndpointFromInclude(resultBundle).findFirst();
	}

	private Stream<Endpoint> getActiveEndpointFromInclude(Bundle resultBundle)
	{
		return resultBundle.getEntry().stream().filter(BundleEntryComponent::hasSearch)
				.filter(e -> SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
				.filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.filter(r -> r instanceof Endpoint).map(r -> (Endpoint) r)
				.filter(e -> EndpointStatus.ACTIVE.equals(e.getStatus()));
	}

	@Override
	public List<Endpoint> getEndpoints(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole)
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
				Collections.singletonList("OrganizationAffiliation:endpoint"));

		return search(OrganizationAffiliation.class, parameters, SearchEntryMode.INCLUDE, Endpoint.class,
				e -> EndpointStatus.ACTIVE.equals(e.getStatus()));
	}
}
