package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.PractitionerDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public class PractitionerAuthorizationRule extends AbstractMetaTagAuthorizationRule<Practitioner, PractitionerDao>
{
	public PractitionerAuthorizationRule(DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		super(Practitioner.class, daoProvider, serverBase, referenceResolver, organizationProvider, readAccessHelper,
				parameterConverter);
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity,
			Practitioner newResource)
	{
		return newResourceOk(connection, identity, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity,
			Practitioner newResource)
	{
		return newResourceOk(connection, identity, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, Identity identity, Practitioner newResource)
	{
		List<String> errors = new ArrayList<String>();

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("Practitioner is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, Practitioner newResource)
	{
		// no unique criteria for Practitioner
		return false;
	}

	@Override
	protected boolean modificationsOk(Connection connection, Practitioner oldResource, Practitioner newResource)
	{
		// no unique criteria for Practitioner
		return true;
	}
}
