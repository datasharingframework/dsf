package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public class StructureDefinitionAuthorizationRule
		extends AbstractMetaTagAuthorizationRule<StructureDefinition, StructureDefinitionDao>
{
	private static final Logger logger = LoggerFactory.getLogger(StructureDefinitionAuthorizationRule.class);

	public StructureDefinitionAuthorizationRule(DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		super(StructureDefinition.class, daoProvider, serverBase, referenceResolver, organizationProvider,
				readAccessHelper, parameterConverter);
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity,
			StructureDefinition newResource)
	{
		return newResourceOk(connection, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity,
			StructureDefinition newResource)
	{
		return newResourceOk(connection, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, StructureDefinition newResource)
	{
		List<String> errors = new ArrayList<>();

		if (newResource.hasStatus())
		{
			if (!EnumSet.of(PublicationStatus.DRAFT, PublicationStatus.ACTIVE, PublicationStatus.RETIRED)
					.contains(newResource.getStatus()))
			{
				errors.add("StructureDefinition.status not one of DRAFT, ACTIVE or RETIRED");
			}
		}
		else
		{
			errors.add("StructureDefinition.status not defined");
		}

		if (!newResource.hasUrl())
		{
			errors.add("StructureDefinition.url not defined");
		}
		if (!newResource.hasVersion())
		{
			errors.add("StructureDefinition.version not defined");
		}

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, StructureDefinition newResource)
	{
		try
		{
			return getDao()
					.readByUrlAndVersionWithTransaction(connection, newResource.getUrl(), newResource.getVersion())
					.map(s -> true).orElse(false);
		}
		catch (SQLException e)
		{
			logger.debug("Error while searching for StructureDefinition", e);
			logger.warn("Error while searching for StructureDefinition: {} - {}", e.getClass().getName(),
					e.getMessage());

			return false;
		}
	}

	@Override
	protected boolean modificationsOk(Connection connection, StructureDefinition oldResource,
			StructureDefinition newResource)
	{
		return oldResource.getUrl().equals(newResource.getUrl())
				&& oldResource.getVersion().equals(newResource.getVersion());
	}
}
