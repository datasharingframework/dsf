package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.service.ResourceReference;

public class BinaryAuthorizationRule extends AbstractMetaTagAuthorizationRule<Binary, BinaryDao>
{
	private final Map<Class<? extends Resource>, AuthorizationRule<?>> rules;

	public BinaryAuthorizationRule(DaoProvider daoProvider, String serverBase, ReferenceResolver referenceResolver,
			OrganizationProvider organizationProvider, ReadAccessHelper readAccessHelper,
			ParameterConverter parameterConverter, AuthorizationRule<?>... supportedSecurityContextRules)
	{
		super(Binary.class, daoProvider, serverBase, referenceResolver, organizationProvider, readAccessHelper,
				parameterConverter);

		this.rules = Arrays.stream(supportedSecurityContextRules)
				.collect(Collectors.toMap(AuthorizationRule::getResourceType, Function.identity()));
	}


	@Override
	protected Optional<String> newResourceOkForCreate(Connection connection, Identity identity, Binary newResource)
	{
		return newResourceOk(connection, identity, newResource);
	}

	@Override
	protected Optional<String> newResourceOkForUpdate(Connection connection, Identity identity, Binary newResource)
	{
		return newResourceOk(connection, identity, newResource);
	}

	private Optional<String> newResourceOk(Connection connection, Identity identity, Binary newResource)
	{
		List<String> errors = new ArrayList<String>();

		boolean hasValidReadAccessTag = hasValidReadAccessTag(connection, newResource);
		boolean hasValidSecurityContext = hasValidSecurityContext(connection, identity, newResource);

		if (!hasValidReadAccessTag && !hasValidSecurityContext)
		{
			errors.add("Binary is missing a valid read access tag or a valid securityContext");
		}

		if (hasValidReadAccessTag && hasValidSecurityContext)
		{
			errors.add("Binary cannot have a valid read access tag and a valid securityContext");
		}

		if (errors.isEmpty())
			return Optional.empty();
		else
			return Optional.of(errors.stream().collect(Collectors.joining(", ")));
	}

	private boolean hasValidSecurityContext(Connection connection, Identity identity, Binary newResource)
	{
		if (newResource != null && newResource.hasSecurityContext())
		{
			Optional<ResourceReference> ref = createIfLiteralInternalOrLogicalReference("Binary.securityContext",
					newResource.getSecurityContext());
			Optional<Resource> securityContextOpt = ref
					.flatMap(r -> referenceResolver.resolveReference(identity, r, connection));

			return securityContextOpt.isPresent() && rules.containsKey(securityContextOpt.get().getClass());
		}
		else
		{
			return false;
		}
	}

	@Override
	protected boolean resourceExists(Connection connection, Binary newResource)
	{
		// no unique criteria for Binary
		return false;
	}

	@Override
	protected boolean modificationsOk(Connection connection, Binary oldResource, Binary newResource)
	{
		// no unique criteria for Binary
		return true;
	}
}
