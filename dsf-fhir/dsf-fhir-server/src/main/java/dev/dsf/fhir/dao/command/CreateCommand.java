package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.jdbc.LargeObjectManager;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;
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

public class CreateCommand<R extends Resource, D extends ResourceDao<R>> extends AbstractCommandWithResource<R, D>
		implements ModifyingCommand
{
	private static final Logger logger = LoggerFactory.getLogger(CreateCommand.class);

	protected final ResponseGenerator responseGenerator;
	protected final ReferenceCleaner referenceCleaner;
	protected final EventGenerator eventGenerator;

	protected R createdResource;
	protected Response responseResult;
	protected ValidationResult validationResult;

	public CreateCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper, R resource, D dao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			ResponseGenerator responseGenerator, ReferenceExtractor referenceExtractor,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner, EventGenerator eventGenerator)
	{
		super(2, index, identity, returnType, bundle, entry, serverBase, authorizationHelper, resource, dao,
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

		// check standard create request url: e.g. Patient
		if (eruComponentes.getPathSegments().size() == 1 && eruComponentes.getQueryParams().isEmpty())
		{
			if (!entry.hasFullUrl() || !entry.getFullUrl().startsWith(URL_UUID_PREFIX))
			{
				Response response = responseGenerator.badCreateRequestUrl(index, entry.getRequest().getUrl());
				throw new WebApplicationException(response);
			}
			else if (resource.hasIdElement() && !resource.getIdElement().getValue().startsWith(URL_UUID_PREFIX))
			{
				Response response = responseGenerator.bundleEntryBadResourceId(index, getResourceTypeName(),
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
			addToIdTranslationTable(idTranslationTable, connection);
		}

		// all other request urls
		else
		{
			Response response = responseGenerator.badCreateRequestUrl(index, entry.getRequest().getUrl());
			throw new WebApplicationException(response);
		}
	}

	private void addToIdTranslationTable(Map<String, IdType> idTranslationTable, Connection connection)
	{
		Optional<Resource> exists = checkAlreadyExists(connection, entry.getRequest().getIfNoneExist(),
				getResourceTypeName());
		if (exists.isEmpty())
		{
			UUID id = UUID.randomUUID();
			idTranslationTable.put(entry.getFullUrl(), new IdType(getResourceTypeName(), id.toString()));
		}
		else
		{
			Resource existingResource = exists.get();
			idTranslationTable.put(entry.getFullUrl(), new IdType(existingResource.getResourceType().toString(),
					existingResource.getIdElement().getIdPart()));
			responseResult = responseGenerator.oneExists(existingResource, entry.getRequest().getIfNoneExist());
		}
	}

	@Override
	public void execute(Map<String, IdType> idTranslationTable, LargeObjectManager largeObjectManager,
			Connection connection, ValidationHelper validationHelper) throws SQLException, WebApplicationException
	{
		// always resolve temp and conditional references, necessary if conditional create and resource exists
		referencesHelper.resolveTemporaryAndConditionalReferencesOrLiteralInternalRelatedArtifactOrAttachmentUrls(
				idTranslationTable, connection);

		// checking again if resource exists, could be that a previous command created, or deleted it
		Optional<Resource> exists = checkAlreadyExists(connection, entry.getRequest().getIfNoneExist(),
				getResourceTypeName());
		if (exists.isEmpty())
		{
			responseResult = null;

			validationResult = validationHelper.checkResourceValidForCreate(identity, resource);

			referencesHelper.resolveLogicalReferences(connection);

			authorizationHelper.checkCreateAllowed(index, connection, identity, resource);

			createdResource = createWithTransactionAndId(largeObjectManager, connection, resource,
					getId(idTranslationTable));
		}
		else if (responseResult == null)
		{
			responseResult = responseGenerator.oneExists(exists.get(), entry.getRequest().getIfNoneExist());
		}
	}

	protected R createWithTransactionAndId(LargeObjectManager largeObjectManager, Connection connection, R resource,
			UUID uuid) throws SQLException
	{
		return dao.createWithTransactionAndId(largeObjectManager, connection, resource, uuid);
	}

	private UUID getId(Map<String, IdType> idTranslationTable)
	{
		IdType idType = idTranslationTable.get(entry.getFullUrl());
		if (idType != null)
		{
			Optional<UUID> uuid = parameterConverter.toUuid(idType.getIdPart());
			if (uuid.isPresent())
				return uuid.get();
		}

		throw new RuntimeException("Error while retrieving id from id translation table");
	}

	private Optional<Resource> checkAlreadyExists(Connection connection, String ifNoneExist, String resourceTypeName)
			throws WebApplicationException
	{
		if (ifNoneExist == null)
			return Optional.empty();

		if (ifNoneExist.isBlank())
		{
			Response response = responseGenerator.badIfNoneExistHeaderValue("blank", ifNoneExist);
			throw new WebApplicationException(response);
		}

		if (!ifNoneExist.contains("?"))
			ifNoneExist = '?' + ifNoneExist;

		UriComponents componentes = UriComponentsBuilder.fromUriString(ifNoneExist).build();
		String path = componentes.getPath();
		if (path != null && !path.isBlank())
		{
			Response response = responseGenerator.badIfNoneExistHeaderValue("no resource", ifNoneExist);
			throw new WebApplicationException(response);
		}

		Map<String, List<String>> queryParameters = parameterConverter
				.urlDecodeQueryParameters(componentes.getQueryParams());
		if (Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(queryParameters::containsKey))
		{
			logger.warn(
					"{} Header contains query parameter not applicable in this conditional create context: '{}', parameters {} will be ignored",
					Constants.HEADER_IF_NONE_EXIST, ifNoneExist, Arrays.toString(SearchQuery.STANDARD_PARAMETERS));

			queryParameters = queryParameters.entrySet().stream()
					.filter(e -> !Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(p -> p.equals(e.getKey())))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		}

		SearchQuery<R> query = dao.createSearchQueryWithoutUserFilter(PageAndCount.single());
		query.configureParameters(queryParameters);

		List<SearchQueryParameterError> unsupportedQueryParameters = query.getUnsupportedQueryParameters();
		if (!unsupportedQueryParameters.isEmpty())
		{
			Response response = responseGenerator.badIfNoneExistHeaderValue(ifNoneExist, unsupportedQueryParameters);
			throw new WebApplicationException(response);
		}

		PartialResult<R> result = exceptionHandler
				.handleSqlException(() -> dao.searchWithTransaction(connection, query));
		if (result.getTotal() == 1)
			return Optional.of(result.getPartialResult().get(0));
		else if (result.getTotal() > 1)
		{
			Response response = responseGenerator.multipleExists(resourceTypeName, ifNoneExist);
			throw new WebApplicationException(response);
		}

		return Optional.empty();
	}

	@Override
	public Optional<BundleEntryComponent> postExecute(Connection connection, EventHandler eventHandler)
	{
		if (responseResult == null)
		{
			// retrieving the latest resource from db to include updated references
			Resource createdResourceWithResolvedReferences = latestOrErrorIfDeletedOrNotFound(connection,
					createdResource);
			try
			{
				referenceCleaner.cleanLiteralReferences(createdResourceWithResolvedReferences);
				eventHandler.handleEvent(eventGenerator.newResourceCreatedEvent(createdResourceWithResolvedReferences));
			}
			catch (Exception e)
			{
				logger.debug("Error while handling resource created event", e);
				logger.warn("Error while handling resource created event: {} - {}", e.getClass().getName(),
						e.getMessage());
			}

			IdType location = createdResourceWithResolvedReferences.getIdElement().withServerBase(serverBase,
					createdResourceWithResolvedReferences.getResourceType().name());

			BundleEntryComponent resultEntry = new BundleEntryComponent();
			resultEntry.setFullUrl(location.toVersionless().toString());

			if (PreferReturnType.REPRESENTATION.equals(returnType))
				resultEntry.setResource(createdResourceWithResolvedReferences);
			else if (PreferReturnType.OPERATION_OUTCOME.equals(returnType))
			{
				OperationOutcome outcome = responseGenerator.created(location.toString(),
						createdResourceWithResolvedReferences);
				validationResult.populateOperationOutcome(outcome);
				resultEntry.getResponse().setOutcome(outcome);
			}

			BundleEntryResponseComponent response = resultEntry.getResponse();
			response.setStatus(Status.CREATED.getStatusCode() + " " + Status.CREATED.getReasonPhrase());
			response.setLocation(location.getValue());
			response.setEtag(RuntimeDelegate.getInstance().createHeaderDelegate(EntityTag.class)
					.toString(new EntityTag(createdResourceWithResolvedReferences.getMeta().getVersionId(), true)));
			response.setLastModified(createdResourceWithResolvedReferences.getMeta().getLastUpdated());

			return Optional.of(resultEntry);
		}
		else
		{
			BundleEntryComponent resultEntry = new BundleEntryComponent();
			BundleEntryResponseComponent response = resultEntry.getResponse();
			response.setStatus(responseResult.getStatusInfo().getStatusCode() + " "
					+ responseResult.getStatusInfo().getReasonPhrase());

			if (responseResult.getLocation() != null)
				response.setLocation(responseResult.getLocation().toString());
			if (responseResult.getEntityTag() != null)
				response.setEtag(responseResult.getEntityTag().getValue());
			if (responseResult.getLastModified() != null)
				response.setLastModified(responseResult.getLastModified());

			return Optional.of(resultEntry);
		}
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
