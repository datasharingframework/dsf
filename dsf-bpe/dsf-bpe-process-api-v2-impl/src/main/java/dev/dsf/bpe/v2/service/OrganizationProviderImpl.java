/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.v2.service;

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

	public OrganizationProviderImpl(DsfClientProvider clientProvider, String localEndpointAddress)
	{
		super(clientProvider, localEndpointAddress);
	}

	@Override
	public Optional<Organization> getLocalOrganization()
	{
		Bundle resultBundle = clientProvider.getLocalDsfClient().searchWithStrictHandling(Endpoint.class,
				Map.of("status", List.of("active"), "address", List.of(localEndpointAddress), "_include",
						List.of("Endpoint:organization")));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getEntry().size() != 2
				|| resultBundle.getEntry().get(0).getResource() == null
				|| !(resultBundle.getEntry().get(0).getResource() instanceof Endpoint)
				|| resultBundle.getEntry().get(1).getResource() == null
				|| !(resultBundle.getEntry().get(1).getResource() instanceof Organization))
		{
			logger.warn("No active (or more than one) endpoint found for address '{}'", localEndpointAddress);
			return Optional.empty();
		}
		else if (getActiveOrganizationFromIncludes(resultBundle).count() != 1)
		{
			logger.warn("No active (or more than one) organization found by active endpoint with address '{}'",
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

		Bundle resultBundle = clientProvider.getLocalDsfClient().searchWithStrictHandling(Organization.class,
				Map.of("active", List.of("true"), "identifier", List.of(organizationIdSp)));

		if (resultBundle == null || resultBundle.getEntry() == null || resultBundle.getTotal() != 1
				|| resultBundle.getEntryFirstRep().getResource() == null
				|| !(resultBundle.getEntryFirstRep().getResource() instanceof Organization))
		{
			logger.warn("No active (or more than one) organization found for identifier '{}'", organizationIdSp);
			return Optional.empty();
		}

		return Optional.of((Organization) resultBundle.getEntryFirstRep().getResource());
	}

	@Override
	public List<Organization> getOrganizations(Identifier parentOrganizationIdentifier)
	{
		if (parentOrganizationIdentifier == null)
		{
			logger.debug("Parent organization identifier is null");
			return List.of();
		}

		String parentOrganizationIdSp = toSearchParameter(parentOrganizationIdentifier);

		Map<String, List<String>> parameters = Map.of("active", List.of("true"), "primary-organization:identifier",
				List.of(parentOrganizationIdSp), "_include",
				List.of("OrganizationAffiliation:participating-organization"));

		return search(OrganizationAffiliation.class, parameters, SearchEntryMode.INCLUDE, Organization.class,
				Organization::getActive);
	}

	@Override
	public List<Organization> getOrganizations(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole)
	{
		if (parentOrganizationIdentifier == null)
		{
			logger.debug("Parent organization identifier is null");
			return List.of();
		}
		else if (memberOrganizationRole == null)
		{
			logger.debug("Member organization role is null");
			return List.of();
		}

		String parentOrganizationIdSp = toSearchParameter(parentOrganizationIdentifier);
		String memberOrganizationRoleSp = toSearchParameter(memberOrganizationRole);

		Map<String, List<String>> parameters = Map.of("active", List.of("true"), "primary-organization:identifier",
				List.of(parentOrganizationIdSp), "role", List.of(memberOrganizationRoleSp), "_include",
				List.of("OrganizationAffiliation:participating-organization"));

		return search(OrganizationAffiliation.class, parameters, SearchEntryMode.INCLUDE, Organization.class,
				Organization::getActive);
	}

	@Override
	public List<Organization> getRemoteOrganizations()
	{
		Optional<Identifier> localOrganizationIdentifier = getLocalOrganizationIdentifier();

		if (localOrganizationIdentifier.isEmpty())
		{
			logger.debug("Local organization identifier unknown");
			return List.of();
		}

		Map<String, List<String>> searchParameters = Map.of("active", List.of("true"), "identifier:not",
				List.of(toSearchParameter(localOrganizationIdentifier.get())), "_profile",
				List.of("http://dsf.dev/fhir/StructureDefinition/organization"));
		return search(Organization.class, searchParameters, SearchEntryMode.MATCH, Organization.class, _ -> true);
	}

	@Override
	public List<Organization> getParentOrganizations()
	{
		Map<String, List<String>> searchParameters = Map.of("active", List.of("true"), "_profile",
				List.of("http://dsf.dev/fhir/StructureDefinition/organization-parent"));
		return search(Organization.class, searchParameters, SearchEntryMode.MATCH, Organization.class, _ -> true);
	}
}
