package dev.dsf.fhir.authorization;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.ConceptDefinitionComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;
import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.service.ResourceReference;
import dev.dsf.fhir.service.ResourceReference.ReferenceType;

public abstract class AbstractAuthorizationRule<R extends Resource, D extends ResourceDao<R>>
		implements AuthorizationRule<R>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractAuthorizationRule.class);

	protected static final String ORGANIZATION_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/organization-identifier";

	protected final Class<R> resourceType;
	protected final DaoProvider daoProvider;
	protected final String serverBase;
	protected final ReferenceResolver referenceResolver;
	protected final OrganizationProvider organizationProvider;
	protected final ReadAccessHelper readAccessHelper;
	protected final ParameterConverter parameterConverter;

	protected final FhirServerRole createRole;
	protected final FhirServerRole readRole;
	protected final FhirServerRole updateRole;
	protected final FhirServerRole deleteRole;
	protected final FhirServerRole historyRole;
	protected final FhirServerRole searchRole;
	protected final FhirServerRole permanentDeleteRole;
	protected final FhirServerRole websocketRole;

	public AbstractAuthorizationRule(Class<R> resourceType, DaoProvider daoProvider, String serverBase,
			ReferenceResolver referenceResolver, OrganizationProvider organizationProvider,
			ReadAccessHelper readAccessHelper, ParameterConverter parameterConverter)
	{
		this.resourceType = resourceType;
		this.daoProvider = daoProvider;
		this.serverBase = serverBase;
		this.referenceResolver = referenceResolver;
		this.organizationProvider = organizationProvider;
		this.readAccessHelper = readAccessHelper;
		this.parameterConverter = parameterConverter;

		createRole = FhirServerRoleImpl.create(resourceType);
		readRole = FhirServerRoleImpl.read(resourceType);
		updateRole = FhirServerRoleImpl.update(resourceType);
		deleteRole = FhirServerRoleImpl.delete(resourceType);
		historyRole = FhirServerRoleImpl.history(resourceType);
		searchRole = FhirServerRoleImpl.search(resourceType);
		permanentDeleteRole = FhirServerRoleImpl.permanentDelete(resourceType);
		websocketRole = FhirServerRoleImpl.websocket(resourceType);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(daoProvider, "daoProvider");
		Objects.requireNonNull(serverBase, "serverBase");
		Objects.requireNonNull(referenceResolver, "referenceResolver");
		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(readAccessHelper, "readAccessHelper");
		Objects.requireNonNull(parameterConverter, "parameterConverter");
	}

	@Override
	public Class<R> getResourceType()
	{
		return resourceType;
	}

	protected String getResourceTypeName()
	{
		return getResourceType().getAnnotation(ResourceDef.class).name();
	}

	@SuppressWarnings("unchecked")
	protected final D getDao()
	{
		return (D) daoProvider.getDao(resourceType).orElseThrow();
	}

	@Override
	public final Optional<String> reasonCreateAllowed(Identity identity, R newResource)
	{
		try (Connection connection = daoProvider.newReadOnlyAutoCommitTransaction())
		{
			return reasonCreateAllowed(connection, identity, newResource);
		}
		catch (SQLException e)
		{
			logger.debug("Error while accessing database", e);
			logger.warn("Error while accessing database: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	@Override
	public final Optional<String> reasonReadAllowed(Identity identity, R existingResource)
	{
		try (Connection connection = daoProvider.newReadOnlyAutoCommitTransaction())
		{
			return reasonReadAllowed(connection, identity, existingResource);
		}
		catch (SQLException e)
		{
			logger.debug("Error while accessing database", e);
			logger.warn("Error while accessing database: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	@Override
	public final Optional<String> reasonUpdateAllowed(Identity identity, R oldResource, R newResource)
	{
		try (Connection connection = daoProvider.newReadOnlyAutoCommitTransaction())
		{
			return reasonUpdateAllowed(connection, identity, oldResource, newResource);
		}
		catch (SQLException e)
		{
			logger.debug("Error while accessing database", e);
			logger.warn("Error while accessing database: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	@Override
	public final Optional<String> reasonDeleteAllowed(Identity identity, R oldResource)
	{
		try (Connection connection = daoProvider.newReadOnlyAutoCommitTransaction())
		{
			return reasonDeleteAllowed(connection, identity, oldResource);
		}
		catch (SQLException e)
		{
			logger.debug("Error while accessing database", e);
			logger.warn("Error while accessing database: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	protected final boolean organizationWithIdentifierExists(Connection connection, Identifier organizationIdentifier)
	{
		String iSystem = organizationIdentifier.getSystem();
		String iValue = organizationIdentifier.getValue();

		Map<String, List<String>> queryParameters = Map.of("identifier", List.of(iSystem + "|" + iValue));
		OrganizationDao dao = daoProvider.getOrganizationDao();
		SearchQuery<Organization> query = dao.createSearchQueryWithoutUserFilter(PageAndCount.exists())
				.configureParameters(queryParameters);

		List<SearchQueryParameterError> uQp = query.getUnsupportedQueryParameters();
		if (!uQp.isEmpty())
		{
			logger.warn("Unable to search for Organization: Unsupported query parameters: {}", uQp);

			throw new IllegalStateException("Unable to search for Organization: Unsupported query parameters.");
		}

		try
		{
			PartialResult<Organization> result = dao.searchWithTransaction(connection, query);
			return result.getTotal() >= 1;
		}
		catch (SQLException e)
		{
			logger.debug("Unable to search for Organization", e);
			logger.warn("Unable to search for Organization: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException("Unable to search for Organization", e);
		}
	}

	protected final boolean roleExists(Connection connection, Coding coding)
	{
		String cSystem = coding.getSystem();
		String cVersion = coding.getVersion();
		String cCode = coding.getCode();

		Map<String, List<String>> queryParameters = Map.of("url",
				List.of(cSystem + (coding.hasVersion() ? "|" + cVersion : "")));
		CodeSystemDao dao = daoProvider.getCodeSystemDao();
		SearchQuery<CodeSystem> query = dao.createSearchQueryWithoutUserFilter(PageAndCount.single())
				.configureParameters(queryParameters);

		List<SearchQueryParameterError> uQp = query.getUnsupportedQueryParameters();
		if (!uQp.isEmpty())
		{
			logger.warn("Unable to search for CodeSystem: Unsupported query parameters: {}", uQp);

			throw new IllegalStateException("Unable to search for CodeSystem: Unsupported query parameters");
		}

		try
		{
			PartialResult<CodeSystem> result = dao.searchWithTransaction(connection, query);
			return result.getTotal() >= 1 && hasCode(result.getPartialResult().get(0), cCode);
		}
		catch (SQLException e)
		{
			logger.debug("Unable to search for CodeSystem", e);
			logger.warn("Unable to search for CodeSystem: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException("Unable to search for CodeSystem", e);
		}
	}

	private boolean hasCode(CodeSystem codeSystem, String cCode)
	{
		return codeSystem.getConcept().stream().filter(ConceptDefinitionComponent::hasCode)
				.map(ConceptDefinitionComponent::getCode).anyMatch(c -> c.equals(cCode));
	}

	@SafeVarargs
	protected final Optional<ResourceReference> createIfLiteralInternalOrLogicalReference(String referenceLocation,
			Reference reference, Class<? extends Resource>... referenceTypes)
	{
		ResourceReference r = new ResourceReference(referenceLocation, reference, referenceTypes);
		ReferenceType type = r.getType(serverBase);
		if (EnumSet.of(ReferenceType.LITERAL_INTERNAL, ReferenceType.LOGICAL).contains(type))
			return Optional.of(r);
		else
			return Optional.empty();
	}

	@Override
	public Optional<String> reasonPermanentDeleteAllowed(Identity identity, R oldResource)
	{
		try (Connection connection = daoProvider.newReadOnlyAutoCommitTransaction())
		{
			return reasonPermanentDeleteAllowed(connection, identity, oldResource);
		}
		catch (SQLException e)
		{
			logger.debug("Error while accessing database", e);
			logger.warn("Error while accessing database: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	@Override
	public final Optional<String> reasonSearchAllowed(Identity identity)
	{
		if (identity.hasDsfRole(searchRole))
		{
			logger.info("Search of {} authorized for identity '{}'", getResourceTypeName(), identity.getName());

			return Optional.of("Identity has role " + searchRole);
		}
		else
		{
			logger.warn("Search of {} unauthorized for identity '{}', no role {}", getResourceTypeName(),
					identity.getName(), searchRole);

			return Optional.empty();
		}
	}

	@Override
	public final Optional<String> reasonHistoryAllowed(Identity identity)
	{
		if (identity.hasDsfRole(historyRole))
		{
			logger.info("History of {} authorized for identity '{}'", getResourceTypeName(), identity.getName());

			return Optional.of("Identity has role " + historyRole);
		}
		else
		{
			logger.warn("History of {} unauthorized for identity '{}', no role {}", getResourceTypeName(),
					identity.getName(), historyRole);

			return Optional.empty();
		}
	}

	@Override
	public Optional<String> reasonPermanentDeleteAllowed(Connection connection, Identity identity, R oldResource)
	{
		final String resourceId = oldResource.getIdElement().getIdPart();
		final long resourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		if (identity.isLocalIdentity() && identity.hasDsfRole(permanentDeleteRole)
				&& reasonDeleteAllowed(connection, identity, oldResource).isPresent())
		{
			logger.info("Permanent delete of {}/{}/_history/{} authorized for identity '{}'", getResourceTypeName(),
					resourceId, resourceVersion, identity.getName());

			return Optional.of("Identity is local identity and has role " + permanentDeleteRole);
		}
		else
		{
			logger.warn(
					"Permanent delete of {}/{}/_history/{} unauthorized for identity '{}', not a local identity or no role {}",
					getResourceTypeName(), resourceId, resourceVersion, identity.getName(), permanentDeleteRole);

			return Optional.empty();
		}
	}

	@Override
	public Optional<String> reasonWebsocketAllowed(Identity identity, R existingResource)
	{
		try (Connection connection = daoProvider.newReadOnlyAutoCommitTransaction())
		{
			return reasonWebsocketAllowed(connection, identity, existingResource);
		}
		catch (SQLException e)
		{
			logger.debug("Error while accessing database", e);
			logger.warn("Error while accessing database: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	private Optional<String> reasonWebsocketAllowed(Connection connection, Identity identity, R existingResource)
	{
		final UUID resourceId = parameterConverter.toUuid(getResourceTypeName(),
				existingResource.getIdElement().getIdPart());
		final long resourceVersion = existingResource.getIdElement().getVersionIdPartAsLong();

		if (identity.isLocalIdentity() && identity.hasDsfRole(websocketRole))
		{
			logger.info("Websocket access to {}/{}/_history/{} authorized for local identity '{}'",
					getResourceTypeName(), resourceId.toString(), resourceVersion, identity.getName());

			return Optional.of("Identity has role " + websocketRole);
		}
		else
		{
			logger.warn(
					"Websocket access to {}/{}/_history/{} unauthorized for identity '{}', not a local identity or no role {}",
					getResourceTypeName(), resourceId.toString(), resourceVersion, identity.getName(), websocketRole);

			return Optional.empty();
		}
	}
}
