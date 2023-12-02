package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.ValueSetDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public class ValueSetAuthorizationRule extends AbstractMetaTagAuthorizationRule<ValueSet, ValueSetDao>
{
	private static final Logger logger = LoggerFactory.getLogger(ValueSetAuthorizationRule.class);

	public ValueSetAuthorizationRule(DaoProvider daoProvider, String serverBase, ReferenceResolver referenceResolver,
			OrganizationProvider organizationProvider, ReadAccessHelper readAccessHelper,
			ParameterConverter parameterConverter)
	{
		super(ValueSet.class, daoProvider, serverBase, referenceResolver, organizationProvider, readAccessHelper,
				parameterConverter);
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity, ValueSet newResource)
	{
		return newResourceOk(connection, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity, ValueSet newResource)
	{
		return newResourceOk(connection, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, ValueSet newResource)
	{
		List<String> errors = new ArrayList<String>();

		if (newResource.hasStatus())
		{
			if (!EnumSet.of(PublicationStatus.DRAFT, PublicationStatus.ACTIVE, PublicationStatus.RETIRED)
					.contains(newResource.getStatus()))
			{
				errors.add("ValueSet.status not one of DRAFT, ACTIVE or RETIRED");
			}
		}
		else
		{
			errors.add("ValueSet.status not defined");
		}

		if (!newResource.hasUrl())
		{
			errors.add("ValueSet.url not defined");
		}
		if (!newResource.hasVersion())
		{
			errors.add("ValueSet.version not defined");
		}

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("ValueSet is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, ValueSet newResource)
	{
		try
		{
			return getDao()
					.readByUrlAndVersionWithTransaction(connection, newResource.getUrl(), newResource.getVersion())
					.isPresent();
		}
		catch (SQLException e)
		{
			logger.warn("Error while searching for ValueSet", e);
			return false;
		}
	}

	@Override
	protected boolean modificationsOk(Connection connection, ValueSet oldResource, ValueSet newResource)
	{
		return oldResource.getUrl().equals(newResource.getUrl())
				&& oldResource.getVersion().equals(newResource.getVersion());
	}
}
