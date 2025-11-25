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
package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.OrganizationAffiliationDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.service.ReferenceResolver;

public class OrganizationAffiliationAuthorizationRule
		extends AbstractMetaTagAuthorizationRule<OrganizationAffiliation, OrganizationAffiliationDao>
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationAffiliationAuthorizationRule.class);

	public OrganizationAffiliationAuthorizationRule(DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		super(OrganizationAffiliation.class, daoProvider, serverBase, referenceResolver, organizationProvider,
				readAccessHelper, parameterConverter);
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity,
			OrganizationAffiliation newResource)
	{
		return newResourceOk(connection, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity,
			OrganizationAffiliation newResource)
	{
		return newResourceOk(connection, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, OrganizationAffiliation newResource)
	{
		List<String> errors = new ArrayList<>();

		if (newResource.hasOrganization())
		{
			if (!newResource.getOrganization().hasReference())
			{
				errors.add("OrganizationAffiliation.organization.reference missing");
			}
		}
		else
		{
			errors.add("OrganizationAffiliation.organization missing");
		}

		if (newResource.hasParticipatingOrganization())
		{
			if (!newResource.getParticipatingOrganization().hasReference())
			{
				errors.add("OrganizationAffiliation.participatingOrganization.reference missing");
			}
		}
		else
		{
			errors.add("OrganizationAffiliation.participatingOrganization missing");
		}

		if (newResource.getEndpoint().size() == 1)
		{
			if (!newResource.getEndpointFirstRep().hasReference())
			{
				errors.add("OrganizationAffiliation.endpoint.reference missing");
			}
		}
		else
		{
			errors.add("OrganizationAffiliation.endpoint missing or more than one");
		}

		if (newResource.hasCode())
		{
			for (int i = 0; i < newResource.getCode().size(); i++)
			{
				if (!newResource.getCode().get(i).hasCoding())
				{
					errors.add("OrganizationAffiliation.code[" + i + "].coding missing");
				}
			}
		}
		else
		{
			errors.add("OrganizationAffiliation.code missing");
		}

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("OrganizationAffiliation is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, OrganizationAffiliation newResource)
	{
		return organizationAffiliationWithParentAndMemberAndEndpointExists(connection, newResource)
				|| organizationAffiliationWithParentAndMemberAndAnyRoleExists(connection, newResource);
	}

	private boolean organizationAffiliationWithParentAndMemberAndEndpointExists(Connection connection,
			OrganizationAffiliation newResource)
	{
		return organizationAffiliationExists(connection,
				queryParameters(newResource, "endpoint", newResource.getEndpointFirstRep().getReference()));
	}

	private boolean organizationAffiliationWithParentAndMemberAndAnyRoleExists(Connection connection,
			OrganizationAffiliation newResource)
	{
		return newResource.getCode().stream().map(CodeableConcept::getCoding).flatMap(List::stream)
				.anyMatch(role -> organizationAffiliationExists(connection,
						queryParameters(newResource, "role", role.getSystem() + "|" + role.getCode())));
	}

	private Map<String, List<String>> queryParameters(OrganizationAffiliation newResource, String param, String value)
	{
		return Map.of("primary-organization", List.of(newResource.getOrganization().getReference()),
				"participating-organization", List.of(newResource.getParticipatingOrganization().getReference()), param,
				List.of(value));
	}

	private boolean organizationAffiliationExists(Connection connection, Map<String, List<String>> queryParameters)
	{
		OrganizationAffiliationDao dao = getDao();
		SearchQuery<OrganizationAffiliation> query = dao.createSearchQueryWithoutUserFilter(PageAndCount.exists())
				.configureParameters(queryParameters);

		List<SearchQueryParameterError> uQp = query.getUnsupportedQueryParameters();
		if (!uQp.isEmpty())
		{
			logger.warn("Unable to search for OrganizationAffiliation: Unsupported query parameters: {}", uQp);

			throw new IllegalStateException(
					"Unable to search for OrganizationAffiliation: Unsupported query parameters");
		}

		try
		{
			PartialResult<OrganizationAffiliation> result = dao.searchWithTransaction(connection, query);
			return result.getTotal() >= 1;
		}
		catch (SQLException e)
		{
			logger.debug("Unable to search for OrganizationAffiliation", e);
			logger.warn("Unable to search for OrganizationAffiliation: {} - {}", e.getClass().getName(),
					e.getMessage());

			throw new RuntimeException("Unable to search for OrganizationAffiliation", e);
		}
	}

	@Override
	protected boolean modificationsOk(Connection connection, OrganizationAffiliation oldResource,
			OrganizationAffiliation newResource)
	{
		return isParentSame(oldResource, newResource) && isMemberSame(oldResource, newResource)
				&& isEndpointSame(oldResource, newResource)
				&& !organizationAffiliationWithParentAndMemberAndAnyRoleAndNotEndpointExists(connection, newResource);
	}

	private boolean organizationAffiliationWithParentAndMemberAndAnyRoleAndNotEndpointExists(Connection connection,
			OrganizationAffiliation newResource)
	{
		return newResource.getCode().stream().map(CodeableConcept::getCoding).flatMap(List::stream).anyMatch(
				organizationAffiliationWithParentAndMemberAndRoleAndNotEndpointExists(connection, newResource));
	}

	private Predicate<Coding> organizationAffiliationWithParentAndMemberAndRoleAndNotEndpointExists(
			Connection connection, OrganizationAffiliation newResource)
	{
		return role ->
		{
			try
			{
				return getDao().existsNotDeletedByParentOrganizationMemberOrganizationRoleAndNotEndpointWithTransaction(
						connection,
						parameterConverter.toUuid(ResourceType.Organization.name(),
								newResource.getOrganization().getReferenceElement().getIdPart()),
						parameterConverter.toUuid(ResourceType.Organization.name(),
								newResource.getParticipatingOrganization().getReferenceElement().getIdPart()),
						role.getSystem(), role.getCode(), parameterConverter.toUuid(ResourceType.Endpoint.name(),
								newResource.getEndpointFirstRep().getReferenceElement().getIdPart()));
			}
			catch (SQLException e)
			{
				logger.debug("Unable to search for OrganizationAffiliation", e);
				logger.warn("Unable to search for OrganizationAffiliation: {} - {}", e.getClass().getName(),
						e.getMessage());

				throw new RuntimeException("Unable to search for OrganizationAffiliation", e);
			}
		};
	}

	private boolean isParentSame(OrganizationAffiliation oldResource, OrganizationAffiliation newResource)
	{
		return oldResource.getOrganization().getReference().equals(newResource.getOrganization().getReference());
	}

	private boolean isMemberSame(OrganizationAffiliation oldResource, OrganizationAffiliation newResource)
	{
		return oldResource.getParticipatingOrganization().getReference()
				.equals(newResource.getParticipatingOrganization().getReference());
	}

	private boolean isEndpointSame(OrganizationAffiliation oldResource, OrganizationAffiliation newResource)
	{
		return oldResource.getEndpointFirstRep().getReference()
				.equals(newResource.getEndpointFirstRep().getReference());
	}
}
