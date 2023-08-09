package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.OrganizationAffiliationDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
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
		return newResourceOk(connection, identity, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity,
			OrganizationAffiliation newResource)
	{
		return newResourceOk(connection, identity, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, Identity identity,
			OrganizationAffiliation newResource)
	{
		List<String> errors = new ArrayList<String>();

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

		if (newResource.hasEndpoint())
		{
			for (int i = 0; i < newResource.getEndpoint().size(); i++)
			{
				if (!newResource.getEndpoint().get(i).hasReference())
				{
					errors.add("OrganizationAffiliation.endpoint[" + i + "].reference missing");
				}
			}
		}
		else
		{
			errors.add("OrganizationAffiliation.endpoint missing");
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
		return organizationAffiliationWithParentAndMemberAndRoleExists(connection, newResource);
	}

	private boolean organizationAffiliationWithParentAndMemberAndRoleExists(Connection connection,
			OrganizationAffiliation newResource)
	{
		Map<String, List<String>> queryParameters = Map.of("primary-organization",
				Collections.singletonList(newResource.getOrganization().getReference()), "participating-organization",
				Collections.singletonList(newResource.getParticipatingOrganization().getReference()), "role",
				newResource.getCode().stream().map(CodeableConcept::getCoding).flatMap(List::stream)
						.map(c -> c.getSystem() + "|" + c.getCode()).toList());

		OrganizationAffiliationDao dao = getDao();
		SearchQuery<OrganizationAffiliation> query = dao.createSearchQueryWithoutUserFilter(0, 0)
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
			logger.warn("Unable to search for OrganizationAffiliation", e);
			throw new RuntimeException("Unable to search for OrganizationAffiliation", e);
		}
	}

	@Override
	protected boolean modificationsOk(Connection connection, OrganizationAffiliation oldResource,
			OrganizationAffiliation newResource)
	{
		return isParentSame(oldResource, newResource) && isMemberSame(oldResource, newResource)
				&& !resourceExists(connection, newResource);
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
}
