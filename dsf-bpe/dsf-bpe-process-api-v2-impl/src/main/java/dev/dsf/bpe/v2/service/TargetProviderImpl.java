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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.bpe.v2.constants.NamingSystems.EndpointIdentifier;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.TargetImpl;
import dev.dsf.bpe.v2.variables.Targets;
import dev.dsf.bpe.v2.variables.TargetsImpl;

public class TargetProviderImpl extends AbstractResourceProvider implements TargetProvider
{
	public static class BuilderImpl implements Builder
	{
		private static final record OrganizationAffiliationAndOrganizationAndEndpoint(
				OrganizationAffiliation affiliation, Organization member, Endpoint endpoint)
		{
		}

		private final List<OrganizationAffiliation> affiliations = new ArrayList<>();
		private final Map<String, Organization> organizationsById = new HashMap<>();
		private final Map<String, Endpoint> endpointsById = new HashMap<>();

		private final Set<String> memberOrganizationIdentifiers = new HashSet<>();

		public BuilderImpl(Identifier[] memberOrganizationIdentifiers)
		{
			if (memberOrganizationIdentifiers != null)
				this.memberOrganizationIdentifiers.addAll(Arrays.stream(memberOrganizationIdentifiers)
						.filter(Objects::nonNull).map(this::identifierToString).toList());
		}

		private String identifierToString(Identifier i)
		{
			return i.getSystem() + "|" + i.getValue();
		}

		private Predicate filter;

		protected Target createTarget(String organizationIdentifierValue, String endpointIdentifierValue,
				String endpointAddress, String correlationKey)
		{
			Objects.requireNonNull(organizationIdentifierValue, "organizationIdentifierValue");
			Objects.requireNonNull(endpointIdentifierValue, "endpointIdentifierValue");
			Objects.requireNonNull(endpointAddress, "endpointAddress");

			return new TargetImpl(organizationIdentifierValue, endpointIdentifierValue, endpointAddress,
					correlationKey);
		}

		@Override
		public Targets withCorrelationKey()
		{
			return toTargets(true);
		}

		@Override
		public Targets withoutCorrelationKey()
		{
			return toTargets(false);
		}

		private Targets toTargets(boolean withCorrelationKey)
		{
			List<TargetImpl> targets = affiliations.stream()
					.filter(a -> organizationsById
							.containsKey(a.getParticipatingOrganization().getReferenceElement().getIdPart())
							&& endpointsById.containsKey(a.getEndpointFirstRep().getReferenceElement().getIdPart()))
					.map(a -> new OrganizationAffiliationAndOrganizationAndEndpoint(a,
							organizationsById.get(a.getParticipatingOrganization().getReferenceElement().getIdPart()),
							endpointsById.get(a.getEndpointFirstRep().getReferenceElement().getIdPart())))
					.filter(a -> OrganizationIdentifier.hasIdentifier(a.member)
							&& EndpointIdentifier.hasIdentifier(a.endpoint) && a.endpoint.hasAddressElement()
							&& a.endpoint.getAddressElement().hasValue())
					.filter(a -> memberOrganizationIdentifiers.isEmpty() ? true
							: memberOrganizationIdentifiers.contains(
									OrganizationIdentifier.findFirst(a.member).map(this::identifierToString).get()))
					.filter(a -> filter == null ? true : filter.test(a.affiliation, a.member, a.endpoint)).map(a ->
					{
						String organizationIdentifierValue = OrganizationIdentifier.findFirst(a.member)
								.map(Identifier::getValue).get();
						String endpointIdentifierValue = EndpointIdentifier.findFirst(a.endpoint)
								.map(Identifier::getValue).get();

						return new TargetImpl(organizationIdentifierValue, endpointIdentifierValue,
								a.endpoint.getAddress(), withCorrelationKey ? UUID.randomUUID().toString() : null);
					}).toList();

			return new TargetsImpl(targets);
		}

		@Override
		public Builder filter(Predicate filter)
		{
			this.filter = filter;

			return this;
		}
	}

	public TargetProviderImpl(DsfClientProvider clientProvider, String localEndpointAddress)
	{
		super(clientProvider, localEndpointAddress);
	}

	protected BuilderImpl createBuilder(Identifier... memberOrganizationIdentifier)
	{
		return new BuilderImpl(memberOrganizationIdentifier);
	}

	private Builder toBuilder(Stream<BundleEntryComponent> entries, Identifier... memberOrganizationIdentifier)
	{
		BuilderImpl builder = createBuilder(memberOrganizationIdentifier);

		entries.forEach(c ->
		{
			SearchEntryMode mode = c.getSearch().getMode();

			if (SearchEntryMode.MATCH.equals(mode) && c.getResource() instanceof OrganizationAffiliation a)
				builder.affiliations.add(a);

			else if (SearchEntryMode.INCLUDE.equals(mode))
			{
				if (c.getResource() instanceof Organization o)
					builder.organizationsById.put(o.getIdElement().getIdPart(), o);
				else if (c.getResource() instanceof Endpoint e)
					builder.endpointsById.put(e.getIdElement().getIdPart(), e);
			}
		});

		return builder;
	}

	@Override
	public Builder create(Identifier parentOrganizationIdentifier)
	{
		Objects.requireNonNull(parentOrganizationIdentifier, "parentOrganizationIdentifier");

		Stream<BundleEntryComponent> entries = search(OrganizationAffiliation.class,
				Map.of("active", List.of("true"), "primary-organization:identifier",
						List.of(toSearchParameter(parentOrganizationIdentifier)), "_include",
						List.of("OrganizationAffiliation:endpoint:Endpoint",
								"OrganizationAffiliation:participating-organization:Organization")));

		return toBuilder(entries);
	}

	@Override
	public Builder create(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole)
	{
		Objects.requireNonNull(parentOrganizationIdentifier, "parentOrganizationIdentifier");
		Objects.requireNonNull(memberOrganizationRole, "memberOrganizationRole");

		Stream<BundleEntryComponent> entries = search(OrganizationAffiliation.class,
				Map.of("active", List.of("true"), "primary-organization:identifier",
						List.of(toSearchParameter(parentOrganizationIdentifier)), "_include",
						List.of("OrganizationAffiliation:endpoint:Endpoint",
								"OrganizationAffiliation:participating-organization:Organization"),
						"role", List.of(toSearchParameter(memberOrganizationRole))));

		return toBuilder(entries);
	}

	@Override
	public Builder create(Identifier parentOrganizationIdentifier, Coding memberOrganizationRole,
			Identifier... memberOrganizationIdentifier)
	{
		Objects.requireNonNull(parentOrganizationIdentifier, "parentOrganizationIdentifier");
		Objects.requireNonNull(memberOrganizationRole, "memberOrganizationRole");
		Objects.requireNonNull(memberOrganizationIdentifier, "memberOrganizationIdentifier");

		Stream<BundleEntryComponent> entries = search(OrganizationAffiliation.class,
				Map.of("active", List.of("true"), "primary-organization:identifier",
						List.of(toSearchParameter(parentOrganizationIdentifier)), "_include",
						List.of("OrganizationAffiliation:endpoint:Endpoint",
								"OrganizationAffiliation:participating-organization:Organization"),
						"role", List.of(toSearchParameter(memberOrganizationRole))));

		return toBuilder(entries, memberOrganizationIdentifier);
	}
}
