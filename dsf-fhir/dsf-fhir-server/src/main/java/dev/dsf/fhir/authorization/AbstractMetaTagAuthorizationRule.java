package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.ReadAccessDao;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.service.ReferenceResolver;

public abstract class AbstractMetaTagAuthorizationRule<R extends Resource, D extends ResourceDao<R>>
		extends AbstractAuthorizationRule<R, D> implements AuthorizationRule<R>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractMetaTagAuthorizationRule.class);

	private final ReadAccessDao readAccessDao;

	public AbstractMetaTagAuthorizationRule(Class<R> resourceType, DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		super(resourceType, daoProvider, serverBase, referenceResolver, organizationProvider, readAccessHelper,
				parameterConverter);

		readAccessDao = daoProvider.getReadAccessDao();
	}

	protected final boolean hasValidReadAccessTag(Connection connection, Resource resource)
	{
		return readAccessHelper.isValid(resource,
				organizationIdentifier -> organizationWithIdentifierExists(connection, organizationIdentifier),
				role -> roleExists(connection, role));
	}

	@Override
	public final Optional<String> reasonCreateAllowed(Connection connection, Identity identity, R newResource)
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(FhirServerRole.CREATE))
		{
			Optional<String> errors = newResourceOkForCreate(connection, identity, newResource);
			if (errors.isEmpty())
			{
				if (!resourceExists(connection, newResource))
				{
					logger.info("Create of {} authorized for identity '{}'", getResourceTypeName(), identity.getName());
					return Optional.of("Identity is local identity and has role " + FhirServerRole.CREATE);
				}
				else
				{
					logger.warn("Create of {} unauthorized, unique resource already exists", getResourceTypeName());
					return Optional.empty();
				}
			}
			else
			{
				logger.warn("Create of {} unauthorized, {}", getResourceTypeName(), errors.get());
				return Optional.empty();
			}
		}
		else
		{
			logger.warn("Create of {} unauthorized for identity '{}', not a local identity or no role {}",
					getResourceTypeName(), FhirServerRole.CREATE);
			return Optional.empty();
		}
	}

	protected abstract boolean resourceExists(Connection connection, R newResource);

	protected abstract Optional<String> newResourceOkForCreate(Connection connection, Identity identity, R newResource);

	@Override
	public final Optional<String> reasonReadAllowed(Connection connection, Identity identity, R existingResource)
	{
		final UUID resourceId = parameterConverter.toUuid(getResourceTypeName(),
				existingResource.getIdElement().getIdPart());
		final long resourceVersion = existingResource.getIdElement().getVersionIdPartAsLong();

		if (identity.hasDsfRole(FhirServerRole.READ))
		{
			try
			{
				UUID organizationId = parameterConverter.toUuid("Organization",
						identity.getOrganization().getIdElement().getIdPart());

				List<String> accessTypes = readAccessDao.getAccessTypes(connection, resourceId, resourceVersion,
						identity.isLocalIdentity(), organizationId);

				if (accessTypes.isEmpty())
				{
					logger.warn("Read of {}/{}/_history/{} unauthorized for identity '{}', no matching access tags",
							getResourceTypeName(), resourceId.toString(), resourceVersion, identity.getName(),
							FhirServerRole.READ);
					return Optional.empty();
				}
				else
				{
					String tags = accessTypes.stream().collect(Collectors.joining(", ", "{", "}"));

					logger.info("Read of {}/{}/_history/{} authorized for identity '{}', matching access {} {}",
							getResourceTypeName(), resourceId.toString(), resourceVersion, identity.getName(),
							accessTypes.size() == 1 ? "tag" : "tags", tags);
					return Optional.of("Identity has role " + FhirServerRole.READ + ", matching access "
							+ (accessTypes.size() == 1 ? "tag" : "tags") + " " + tags);
				}
			}
			catch (SQLException e)
			{
				logger.warn("Error while checking read access", e);
				throw new RuntimeException(e);
			}
		}
		else
		{
			logger.warn("Read of {}/{}/_history/{} unauthorized for identity '{}', no role {}", getResourceTypeName(),
					resourceId.toString(), resourceVersion, identity.getName(), FhirServerRole.READ);
			return Optional.empty();
		}
	}

	protected abstract Optional<String> newResourceOkForUpdate(Connection connection, Identity identity, R newResource);

	@Override
	public final Optional<String> reasonUpdateAllowed(Connection connection, Identity identity, R oldResource,
			R newResource)
	{
		final String resourceId = oldResource.getIdElement().getIdPart();
		final long resourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		if (identity.isLocalIdentity() && identity.hasDsfRole(FhirServerRole.UPDATE))
		{
			Optional<String> errors = newResourceOkForUpdate(connection, identity, newResource);
			if (errors.isEmpty())
			{
				if (modificationsOk(connection, oldResource, newResource))
				{
					logger.info("Update of {}/{}/_history/{} authorized for identity '{}'", getResourceTypeName(),
							resourceId.toString(), resourceVersion, identity.getName());
					return Optional.of("Identity is local identity and has role " + FhirServerRole.UPDATE);
				}
				else
				{
					logger.warn("Update of {}/{}/_history/{} unauthorized, modification not allowed",
							getResourceTypeName(), resourceId.toString(), resourceVersion);
					return Optional.empty();
				}
			}
			else
			{
				logger.warn("Update of {}/{}/_history/{} unauthorized, {}", getResourceTypeName(),
						resourceId.toString(), resourceVersion, errors.get());
				return Optional.empty();
			}
		}
		else
		{
			logger.warn(
					"Update of {}/{}/_history/{} unauthorized for identity '{}', not a local identity or no role {}",
					getResourceTypeName(), resourceId.toString(), resourceVersion, identity.getName(),
					FhirServerRole.UPDATE);
			return Optional.empty();
		}
	}

	/**
	 * No need to check if the new resource is valid, will be checked by
	 * {@link #newResourceOkForUpdate(Connection, Identity, Resource)}
	 *
	 * @param connection
	 *            not <code>null</code>
	 * @param oldResource
	 *            not <code>null</code>
	 * @param newResource
	 *            not <code>null</code>
	 * @return <code>true</code> if modifications from <b>oldResource</b> to <b>newResource</b> are ok
	 */
	protected abstract boolean modificationsOk(Connection connection, R oldResource, R newResource);

	@Override
	public final Optional<String> reasonDeleteAllowed(Connection connection, Identity identity, R oldResource)
	{
		final String resourceId = oldResource.getIdElement().getIdPart();
		final long resourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		if (identity.isLocalIdentity() && identity.hasDsfRole(FhirServerRole.DELETE))
		{
			logger.info("Delete of {}/{}/_history/{} authorized for identity '{}'", getResourceTypeName(), resourceId,
					resourceVersion, identity.getName());
			return Optional.of("Identity is local identity and has role " + FhirServerRole.DELETE);
		}
		else
		{
			logger.warn(
					"Delete of {}/{}/_history/{} unauthorized for identity '{}', not a local identity or no role {}",
					getResourceTypeName(), resourceId, resourceVersion, identity.getName(), FhirServerRole.DELETE);
			return Optional.empty();
		}
	}
}
