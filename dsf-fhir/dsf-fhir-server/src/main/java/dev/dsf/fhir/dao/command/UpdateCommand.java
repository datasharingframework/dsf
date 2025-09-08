package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.exception.ResourceVersionNoMatchException;
import dev.dsf.fhir.dao.jdbc.LargeObjectManager;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.event.ResourceUpdatedEvent;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.SnapshotGenerator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.RuntimeDelegate;

//TODO rework log and audit messages
public class UpdateCommand<R extends Resource, D extends ResourceDao<R>> extends AbstractCommandWithResource<R, D>
		implements ModifyingCommand
{
	private static final Logger logger = LoggerFactory.getLogger(UpdateCommand.class);

	protected final ResponseGenerator responseGenerator;
	protected final ReferenceCleaner referenceCleaner;
	protected final EventGenerator eventGenerator;

	protected R updatedResource;
	protected ValidationResult validationResult;

	public UpdateCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper, R resource, D dao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			ResponseGenerator responseGenerator, ReferenceExtractor referenceExtractor,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner, EventGenerator eventGenerator)
	{
		super(3, index, identity, returnType, bundle, entry, serverBase, authorizationHelper, resource, dao,
				exceptionHandler, parameterConverter, responseGenerator, referenceExtractor, referenceResolver);

		this.responseGenerator = responseGenerator;
		this.referenceCleaner = referenceCleaner;

		this.eventGenerator = eventGenerator;
	}

	@Override
	public void preExecute(Map<String, IdType> idTranslationTable, Connection connection,
			ValidationHelper validationHelper, SnapshotGenerator snapshotGenerator)
	{
		UriComponents eruComponentes = UriComponentsBuilder.fromUriString(entry.getRequest().getUrl()).build();

		// check standard update request url: e.g. Patient/123
		if (eruComponentes.getPathSegments().size() == 2 && eruComponentes.getQueryParams().isEmpty())
		{
			if (!entry.hasFullUrl() || entry.getFullUrl().startsWith(URL_UUID_PREFIX))
			{
				Response response = responseGenerator.badUpdateRequestUrl(index, entry.getRequest().getUrl());
				throw new WebApplicationException(response);
			}
			else if (!resource.hasIdElement() || !resource.getIdElement().hasIdPart())
			{
				Response response = responseGenerator.bundleEntryResouceMissingId(index,
						resource.getResourceType().name());
				throw new WebApplicationException(response);
			}
			else if (resource.getIdElement().getIdPart().startsWith(URL_UUID_PREFIX))
			{
				Response response = responseGenerator.badUpdateRequestUrl(index, entry.getRequest().getUrl());
				throw new WebApplicationException(response);
			}

			String expectedBaseUrl = serverBase;
			String expectedResourceTypeName = resource.getResourceType().name();
			String expectedId = resource.getIdElement().getIdPart();
			String expectedfullUrl = new IdType(expectedBaseUrl, expectedResourceTypeName, expectedId, null).getValue();

			if (!expectedfullUrl.equals(entry.getFullUrl()))
			{
				Response response = responseGenerator.badBundleEntryFullUrl(index, entry.getFullUrl());
				throw new WebApplicationException(response);
			}
			else if (!expectedResourceTypeName.equals(eruComponentes.getPathSegments().get(0))
					|| !expectedId.equals(eruComponentes.getPathSegments().get(1)))
			{
				Response response = responseGenerator.badUpdateRequestUrl(index, entry.getRequest().getUrl());
				throw new WebApplicationException(response);
			}
		}

		// check conditional update request url: e.g. Patient?...
		else if (eruComponentes.getPathSegments().size() == 1 && !eruComponentes.getQueryParams().isEmpty())
		{
			if (!entry.getFullUrl().startsWith(URL_UUID_PREFIX))
			{
				Response response = responseGenerator.badUpdateRequestUrl(index, entry.getRequest().getUrl());
				throw new WebApplicationException(response);
			}
			else if (resource.hasIdElement() && !resource.getIdElement().getValue().startsWith(URL_UUID_PREFIX))
			{
				Response response = responseGenerator.bundleEntryBadResourceId(index, resource.getResourceType().name(),
						URL_UUID_PREFIX);
				throw new WebApplicationException(response);
			}
			else if (resource.hasIdElement() && !entry.getFullUrl().equals(resource.getIdElement().getValue()))
			{
				Response response = responseGenerator.badBundleEntryFullUrlVsResourceId(index, entry.getFullUrl(),
						resource.getIdElement().getValue());
				throw new WebApplicationException(response);
			}

			// add new or existing id to the id translation table
			addMissingIdToTranslationTableAndCheckConditionFindsResource(idTranslationTable, connection);
		}

		// all other request urls
		else
		{
			Response response = responseGenerator.badUpdateRequestUrl(index, entry.getRequest().getUrl());
			throw new WebApplicationException(response);
		}
	}

	private boolean addMissingIdToTranslationTableAndCheckConditionFindsResource(Map<String, IdType> idTranslationTable,
			Connection connection)
	{
		UriComponents componentes = UriComponentsBuilder.fromUriString(entry.getRequest().getUrl()).build();
		String resourceTypeName = componentes.getPathSegments().get(0);
		Map<String, List<String>> queryParameters = parameterConverter
				.urlDecodeQueryParameters(componentes.getQueryParams());

		if (Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(queryParameters::containsKey))
		{
			logger.warn(
					"Query contains parameter not applicable in this conditional update context: '{}', parameters {} will be ignored",
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					Arrays.toString(SearchQuery.STANDARD_PARAMETERS));

			queryParameters = queryParameters.entrySet().stream()
					.filter(e -> !Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(p -> p.equals(e.getKey())))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		}

		SearchQuery<R> query = dao.createSearchQueryWithoutUserFilter(PageAndCount.single());
		query.configureParameters(queryParameters);

		List<SearchQueryParameterError> unsupportedParams = query.getUnsupportedQueryParameters();
		if (!unsupportedParams.isEmpty())
		{
			Response response = responseGenerator.unsupportedConditionalUpdateQuery(index, entry.getRequest().getUrl(),
					unsupportedParams);
			throw new WebApplicationException(response);
		}

		PartialResult<R> result = exceptionHandler
				.handleSqlException(() -> dao.searchWithTransaction(connection, query));

		// No matches and no id provided or temp id: The server creates the resource.
		if (result.getTotal() <= 0
				&& (!resource.hasId() || resource.getIdElement().getValue().startsWith(URL_UUID_PREFIX)))
		{
			if (!idTranslationTable.containsKey(entry.getFullUrl()))
			{
				UUID id = UUID.randomUUID();
				idTranslationTable.put(entry.getFullUrl(),
						new IdType(resource.getResourceType().toString(), id.toString()));
			}

			return false;
		}

		// No matches, id provided: The server treats the interaction as an Update as Create interaction (or rejects it,
		// if it does not support Update as Create) -> reject
		else if (result.getTotal() <= 0 && resource.hasId())
		{
			// TODO bundle specific error
			Response response = responseGenerator.updateAsCreateNotAllowed(resourceTypeName);
			throw new WebApplicationException(response);
		}

		// One Match, no resource id provided OR (resource id provided and it matches the found resource):
		// The server performs the update against the matching resource
		else if (result.getTotal() == 1)
		{
			R dbResource = result.getPartialResult().get(0);
			IdType dbResourceId = dbResource.getIdElement();

			// update: resource has no id or resource has temporary id
			if (!resource.hasId() || resource.getIdElement().getValue().startsWith(URL_UUID_PREFIX))
			{
				idTranslationTable.put(entry.getFullUrl(),
						new IdType(resource.getResourceType().toString(), dbResource.getIdElement().getIdPart()));

				return true;
			}
			// update: resource has same id
			else if (resource.hasId()
					&& (!resource.getIdElement().hasBaseUrl()
							|| serverBase.equals(resource.getIdElement().getBaseUrl()))
					&& (!resource.getIdElement().hasResourceType()
							|| resourceTypeName.equals(resource.getIdElement().getResourceType()))
					&& dbResourceId.getIdPart().equals(resource.getIdElement().getIdPart()))
			{
				idTranslationTable.put(entry.getFullUrl(),
						new IdType(resource.getResourceType().toString(), dbResource.getIdElement().getIdPart()));

				return true;
			}
			else
			{
				// TODO bundle specific error
				Response response = responseGenerator.badRequestIdsNotMatching(
						dbResourceId.withServerBase(serverBase, resourceTypeName),
						resource.getIdElement().hasBaseUrl() && resource.getIdElement().hasResourceType()
								? resource.getIdElement()
								: resource.getIdElement().withServerBase(serverBase, resourceTypeName));
				throw new WebApplicationException(response);
			}
		}
		// Multiple matches: The server returns a 412 Precondition Failed error indicating the client's criteria were
		// not selective enough preferably with an OperationOutcome
		else // if (result.getOverallCount() > 1)
		{
			Response response = responseGenerator.multipleExists(resourceTypeName, UriComponentsBuilder.newInstance()
					.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString());
			throw new WebApplicationException(response);
		}
	}

	@Override
	public void execute(Map<String, IdType> idTranslationTable, LargeObjectManager largeObjectManager,
			Connection connection, ValidationHelper validationHelper) throws SQLException, WebApplicationException
	{
		UriComponents components = UriComponentsBuilder.fromUriString(entry.getRequest().getUrl()).build();

		if (components.getPathSegments().size() == 2 && components.getQueryParams().isEmpty())
			updateById(idTranslationTable, largeObjectManager, connection, validationHelper,
					components.getPathSegments().get(0), components.getPathSegments().get(1));
		else if (components.getPathSegments().size() == 1 && !components.getQueryParams().isEmpty())
			updateByCondition(idTranslationTable, largeObjectManager, connection, validationHelper,
					components.getPathSegments().get(0));
		else
		{
			Response response = responseGenerator.badUpdateRequestUrl(index, entry.getRequest().getUrl());
			throw new WebApplicationException(response);
		}
	}

	private void updateById(Map<String, IdType> idTranslationTable, LargeObjectManager largeObjectManager,
			Connection connection, ValidationHelper validationHelper, String resourceTypeName, String pathId)
			throws SQLException
	{
		IdType resourceId = resource.getIdElement();

		if (!Objects.equals(pathId, resourceId.getIdPart()))
		{
			Response response = responseGenerator.pathVsElementIdInBundle(index, resourceTypeName, pathId, resourceId);
			throw new WebApplicationException(response);
		}
		if (resourceId.getBaseUrl() != null && !serverBase.equals(resourceId.getBaseUrl()))
		{
			Response response = responseGenerator.invalidBaseUrlInBundle(index, resourceTypeName, resourceId);
			throw new WebApplicationException(response);
		}

		if (!Objects.equals(resourceTypeName, resource.getResourceType().name()))
		{
			Response response = responseGenerator.nonMatchingResourceTypeAndRequestUrlInBundle(index, resourceTypeName,
					entry.getRequest().getUrl());
			throw new WebApplicationException(response);
		}

		@SuppressWarnings("unchecked")
		R copy = (R) resource.copy();
		checkUpdateAllowed(idTranslationTable, connection, validationHelper, identity, copy);

		Optional<Long> ifMatch = Optional.ofNullable(entry.getRequest().getIfMatch())
				.flatMap(parameterConverter::toEntityTag).flatMap(parameterConverter::toVersion);

		updatedResource = exceptionHandler.handleSqlExAndResourceNotFoundExAndResouceVersionNonMatchEx(resourceTypeName,
				() -> updateWithTransaction(largeObjectManager, connection, resource, ifMatch.orElse(null)));
	}

	protected R updateWithTransaction(LargeObjectManager largeObjectManager, Connection connection, R resource,
			Long expectedVersion) throws SQLException, ResourceNotFoundException, ResourceVersionNoMatchException
	{
		return dao.updateWithTransaction(largeObjectManager, connection, resource, expectedVersion);
	}

	private void checkUpdateAllowed(Map<String, IdType> idTranslationTable, Connection connection,
			ValidationHelper validationHelper, Identity identity, R newResource)
	{
		String resourceTypeName = newResource.getResourceType().name();
		String id = newResource.getIdElement().getIdPart();

		Optional<R> dbResource = exceptionHandler.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
				() -> dao.readWithTransaction(connection, parameterConverter.toUuid(resourceTypeName, id)));

		if (dbResource.isEmpty())
		{
			audit.info("Update as create of non existing {} denied for identity '{}'", resourceTypeName,
					identity.getName());
			Response response = responseGenerator.updateAsCreateNotAllowed(resourceTypeName);
			throw new WebApplicationException(response);
		}
		else
		{
			referencesHelper.resolveTemporaryAndConditionalReferencesOrLiteralInternalRelatedArtifactOrAttachmentUrls(
					idTranslationTable, connection);

			validationResult = validationHelper.checkResourceValidForUpdate(identity, resource);

			referencesHelper.resolveLogicalReferences(connection);

			authorizationHelper.checkUpdateAllowed(index, connection, identity, dbResource.get(), resource);
		}
	}

	private void updateByCondition(Map<String, IdType> idTranslationTable, LargeObjectManager largeObjectManager,
			Connection connection, ValidationHelper validationHelper, String resourceTypeName) throws SQLException
	{

		boolean foundByCondition = addMissingIdToTranslationTableAndCheckConditionFindsResource(idTranslationTable,
				connection);

		// update
		if (foundByCondition)
		{
			resource.setIdElement(getId(idTranslationTable));

			updateById(idTranslationTable, largeObjectManager, connection, validationHelper, resourceTypeName,
					resource.getIdElement().getIdPart());
		}

		// update as create
		else
		{
			referencesHelper.resolveTemporaryAndConditionalReferencesOrLiteralInternalRelatedArtifactOrAttachmentUrls(
					idTranslationTable, connection);

			validationResult = validationHelper.checkResourceValidForCreate(identity, resource);

			referencesHelper.resolveLogicalReferences(connection);

			authorizationHelper.checkCreateAllowed(index, connection, identity, resource);

			updatedResource = createWithTransactionAndId(largeObjectManager, connection, resource,
					getUuid(idTranslationTable));
		}
	}

	protected R createWithTransactionAndId(LargeObjectManager largeObjectManager, Connection connection, R resource,
			UUID uuid) throws SQLException
	{
		return dao.createWithTransactionAndId(largeObjectManager, connection, resource, uuid);
	}

	private IdType getId(Map<String, IdType> idTranslationTable)
	{
		IdType idType = idTranslationTable.get(entry.getFullUrl());
		if (idType != null)
		{
			return idType;
		}

		throw new RuntimeException("Error while retrieving id from id translation table");
	}

	private UUID getUuid(Map<String, IdType> idTranslationTable)
	{
		Optional<UUID> uuid = parameterConverter.toUuid(getId(idTranslationTable).getIdPart());
		if (uuid.isPresent())
			return uuid.get();

		throw new RuntimeException("Error while retrieving id from id translation table");
	}

	@Override
	public Optional<BundleEntryComponent> postExecute(Connection connection, EventHandler eventHandler)
	{
		// retrieving the latest resource from db to include updated references
		R updatedResourceWithResolvedReferences = latestOrErrorIfDeletedOrNotFound(connection, updatedResource);

		referenceCleaner.cleanLiteralReferences(updatedResourceWithResolvedReferences);

		try
		{
			eventHandler.handleEvent(createEvent(updatedResourceWithResolvedReferences));
		}
		catch (Exception e)
		{
			logger.debug("Error while handling resource updated event", e);
			logger.warn("Error while handling resource updated event: {} - {}", e.getClass().getName(), e.getMessage());
		}

		modifyResponseResource(updatedResourceWithResolvedReferences);

		IdType location = updatedResourceWithResolvedReferences.getIdElement().withServerBase(serverBase,
				updatedResourceWithResolvedReferences.getResourceType().name());

		BundleEntryComponent resultEntry = new BundleEntryComponent();
		resultEntry.setFullUrl(location.toVersionless().toString());

		if (PreferReturnType.REPRESENTATION.equals(returnType))
			resultEntry.setResource(updatedResourceWithResolvedReferences);
		else if (PreferReturnType.OPERATION_OUTCOME.equals(returnType))
		{
			OperationOutcome outcome = responseGenerator.updated(location.toString(),
					updatedResourceWithResolvedReferences);
			validationResult.populateOperationOutcome(outcome);
			resultEntry.getResponse().setOutcome(outcome);
		}

		BundleEntryResponseComponent response = resultEntry.getResponse();
		response.setStatus(Status.OK.getStatusCode() + " " + Status.OK.getReasonPhrase());
		response.setLocation(location.getValue());
		response.setEtag(RuntimeDelegate.getInstance().createHeaderDelegate(EntityTag.class)
				.toString(new EntityTag(updatedResourceWithResolvedReferences.getMeta().getVersionId(), true)));
		response.setLastModified(updatedResourceWithResolvedReferences.getMeta().getLastUpdated());

		return Optional.of(resultEntry);
	}

	protected ResourceUpdatedEvent createEvent(Resource eventResource)
	{
		return eventGenerator.newResourceUpdatedEvent(eventResource);
	}

	protected void modifyResponseResource(R responseResource)
	{
	}

	private R latestOrErrorIfDeletedOrNotFound(Connection connection, Resource resource)
	{
		try
		{
			return dao
					.readWithTransaction(connection,
							parameterConverter.toUuid(resource.getResourceType().name(),
									resource.getIdElement().getIdPart()))
					.orElseThrow(() -> new ResourceNotFoundException(resource.getIdElement().getIdPart()));
		}
		catch (ResourceNotFoundException | SQLException | ResourceDeletedException e)
		{
			logger.debug("Error while reading resource from db", e);
			logger.warn("Error while reading resource from db: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	@Override
	public LargeObjectManager createLargeObjectManager(Connection connection)
	{
		return dao.createLargeObjectManager(connection);
	}
}
