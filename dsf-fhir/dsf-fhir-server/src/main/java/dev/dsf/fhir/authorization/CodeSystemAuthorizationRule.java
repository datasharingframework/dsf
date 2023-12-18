package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public class CodeSystemAuthorizationRule extends AbstractMetaTagAuthorizationRule<CodeSystem, CodeSystemDao>
{
	private static final Logger logger = LoggerFactory.getLogger(CodeSystemAuthorizationRule.class);

	public CodeSystemAuthorizationRule(DaoProvider daoProvider, String serverBase, ReferenceResolver referenceResolver,
			OrganizationProvider organizationProvider, ReadAccessHelper readAccessHelper,
			ParameterConverter parameterConverter)
	{
		super(CodeSystem.class, daoProvider, serverBase, referenceResolver, organizationProvider, readAccessHelper,
				parameterConverter);
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity, CodeSystem newResource)
	{
		return newResourceOk(connection, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity, CodeSystem newResource)
	{
		return newResourceOk(connection, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, CodeSystem newResource)
	{
		List<String> errors = new ArrayList<>();

		if (newResource.hasStatus())
		{
			if (!EnumSet.of(PublicationStatus.DRAFT, PublicationStatus.ACTIVE, PublicationStatus.RETIRED)
					.contains(newResource.getStatus()))
			{
				errors.add("CodeSystem.status not one of DRAFT, ACTIVE or RETIRED");
			}
		}
		else
		{
			errors.add("CodeSystem.status not defined");
		}

		if (!newResource.hasUrl())
		{
			errors.add("CodeSystem.url not defined");
		}
		if (!newResource.hasVersion())
		{
			errors.add("CodeSystem.version not defined");
		}

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("CodeSystem is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, CodeSystem newResource)
	{
		try
		{
			return getDao()
					.readByUrlAndVersionWithTransaction(connection, newResource.getUrl(), newResource.getVersion())
					.isPresent();
		}
		catch (SQLException e)
		{
			logger.debug("Error while searching for CodeSystem", e);
			logger.warn("Error while searching for CodeSystem: {} - {}", e.getClass().getName(), e.getMessage());

			return false;
		}
	}

	@Override
	protected boolean modificationsOk(Connection connection, CodeSystem oldResource, CodeSystem newResource)
	{
		return oldResource.getUrl().equals(newResource.getUrl())
				&& oldResource.getVersion().equals(newResource.getVersion());
	}
}
