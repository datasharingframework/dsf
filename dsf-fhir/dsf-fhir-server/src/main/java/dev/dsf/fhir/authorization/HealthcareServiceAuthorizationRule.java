package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.HealthcareService;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.HealthcareServiceDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public class HealthcareServiceAuthorizationRule
		extends AbstractMetaTagAuthorizationRule<HealthcareService, HealthcareServiceDao>
{
	public HealthcareServiceAuthorizationRule(DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		super(HealthcareService.class, daoProvider, serverBase, referenceResolver, organizationProvider,
				readAccessHelper, parameterConverter);
	}

	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity,
			HealthcareService newResource)
	{
		return newResourceOk(connection, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity,
			HealthcareService newResource)
	{
		return newResourceOk(connection, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, HealthcareService newResource)
	{
		List<String> errors = new ArrayList<>();

		if (!hasValidReadAccessTag(connection, newResource))
		{
			errors.add("HealthcareService is missing valid read access tag");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	@Override
	protected boolean resourceExists(Connection connection, HealthcareService newResource)
	{
		// no unique criteria for HealthcareService
		return false;
	}

	@Override
	protected boolean modificationsOk(Connection connection, HealthcareService oldResource,
			HealthcareService newResource)
	{
		// no unique criteria for HealthcareService
		return true;
	}
}
