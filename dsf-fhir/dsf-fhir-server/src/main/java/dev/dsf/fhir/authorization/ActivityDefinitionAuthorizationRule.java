package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.process.ProcessAuthorizationHelper;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.ActivityDefinitionDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public class ActivityDefinitionAuthorizationRule
		extends AbstractMetaTagAuthorizationRule<ActivityDefinition, ActivityDefinitionDao>
{
	private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionAuthorizationRule.class);

	private final ProcessAuthorizationHelper processAuthorizationHelper;

	public ActivityDefinitionAuthorizationRule(DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter,
			ProcessAuthorizationHelper processAuthorizationHelper)
	{
		super(ActivityDefinition.class, daoProvider, serverBase, referenceResolver, organizationProvider,
				readAccessHelper, parameterConverter);

		this.processAuthorizationHelper = processAuthorizationHelper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(processAuthorizationHelper, "processAuthorizationHelper");
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity user,
			ActivityDefinition newResource)
	{
		return newResourceOk(connection, user, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity user,
			ActivityDefinition newResource)
	{
		return newResourceOk(connection, user, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, Identity identity, ActivityDefinition newResource)
	{
		List<String> errors = new ArrayList<String>();

		// TODO check existence of profiles, codes and identifier against DB
		if (!processAuthorizationHelper.isValid(newResource, taskProfile -> true, practitionerRole -> true,
				organizationIdentifier -> true, organizationRole -> true))
		{
			errors.add("ActivityDefinition.extension with url "
					+ ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION
					+ " not valid or missing, at least one expected");
		}

		if (newResource.hasStatus())
		{
			if (!EnumSet.of(PublicationStatus.DRAFT, PublicationStatus.ACTIVE, PublicationStatus.RETIRED)
					.contains(newResource.getStatus()))
			{
				errors.add("ActivityDefinition.status not one of DRAFT, ACTIVE or RETIRED");
			}
		}
		else
		{
			errors.add("ActivityDefinition.status not defined");
		}

		if (!newResource.hasUrl())
		{
			errors.add("ActivityDefinition.url not defined");
		}
		if (!newResource.hasVersion())
		{
			errors.add("ActivityDefinition.version not defined");
		}

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("ActivityDefinition is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, ActivityDefinition newResource)
	{
		try
		{
			return getDao()
					.readByUrlAndVersionWithTransaction(connection, newResource.getUrl(), newResource.getVersion())
					.isPresent();
		}
		catch (SQLException e)
		{
			logger.warn("Error while searching for ActivityDefinition", e);
			return false;
		}
	}

	@Override
	protected boolean modificationsOk(Connection connection, ActivityDefinition oldResource,
			ActivityDefinition newResource)
	{
		return oldResource.getUrl().equals(newResource.getUrl())
				&& oldResource.getVersion().equals(newResource.getVersion());
	}
}
