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
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.exception.ResourceNotFoundException;
import dev.dsf.fhir.dao.provider.DaoProvider;
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
import dev.dsf.fhir.validation.SnapshotGenerator;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

public class DeleteCommand extends AbstractCommand implements ModifyingCommand
{
	private static final Logger logger = LoggerFactory.getLogger(DeleteCommand.class);

	private final ResponseGenerator responseGenerator;
	private final DaoProvider daoProvider;
	private final ExceptionHandler exceptionHandler;
	private final ParameterConverter parameterConverter;
	private final EventGenerator eventGenerator;

	private boolean deleted;
	private String resourceTypeName;
	private Class<? extends Resource> resourceType;
	private String id;

	public DeleteCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper,
			ResponseGenerator responseGenerator, DaoProvider daoProvider, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, EventGenerator eventGenerator)
	{
		super(1, index, identity, returnType, bundle, entry, serverBase, authorizationHelper);

		this.responseGenerator = responseGenerator;
		this.daoProvider = daoProvider;
		this.exceptionHandler = exceptionHandler;
		this.parameterConverter = parameterConverter;
		this.eventGenerator = eventGenerator;
	}

	@Override
	public void execute(Map<String, IdType> idTranslationTable, Connection connection,
			ValidationHelper validationHelper, SnapshotGenerator snapshotGenerator)
			throws SQLException, WebApplicationException
	{
		UriComponents componentes = UriComponentsBuilder.fromUriString(entry.getRequest().getUrl()).build();
		resourceTypeName = componentes.getPathSegments().get(0);

		if (componentes.getPathSegments().size() == 2 && componentes.getQueryParams().isEmpty())
			deleteById(idTranslationTable, connection, componentes.getPathSegments().get(0),
					componentes.getPathSegments().get(1));
		else if (componentes.getPathSegments().size() == 1 && !componentes.getQueryParams().isEmpty())
			deleteByCondition(idTranslationTable, connection, componentes.getPathSegments().get(0),
					parameterConverter.urlDecodeQueryParameters(componentes.getQueryParams()));
		else
			throw new WebApplicationException(
					responseGenerator.badDeleteRequestUrl(index, entry.getRequest().getUrl()));
	}

	private void deleteById(Map<String, IdType> idTranslationTable, Connection connection, String resourceTypeName,
			String id)
	{
		Optional<ResourceDao<?>> optDao = daoProvider.getDao(resourceTypeName);

		if (optDao.isEmpty())
			throw new WebApplicationException(
					responseGenerator.resourceTypeNotSupportedByImplementation(index, resourceTypeName));
		else
		{
			@SuppressWarnings("unchecked")
			ResourceDao<Resource> dao = (ResourceDao<Resource>) optDao.get();
			UUID uuid = parameterConverter.toUuid(resourceTypeName, id);

			Optional<Resource> dbResource = exceptionHandler
					.handleSqlException(() -> dao.readIncludingDeletedWithTransaction(connection, uuid));

			dbResource.ifPresent(
					oldResource -> authorizationHelper.checkDeleteAllowed(index, connection, identity, oldResource));

			deleted = exceptionHandler.handleSqlAndResourceNotFoundException(resourceTypeName,
					() -> deleteWithTransaction(dao, connection, uuid));

			this.resourceType = dao.getResourceType();
			this.id = id;

			setNewIdIfResourceExistsInTranslationTable(idTranslationTable, id);
		}
	}

	private void deleteByCondition(Map<String, IdType> idTranslationTable, Connection connection,
			String resourceTypeName, Map<String, List<String>> queryParameters)
	{
		Optional<ResourceDao<?>> dao = daoProvider.getDao(resourceTypeName);

		if (dao.isEmpty())
			throw new WebApplicationException(
					responseGenerator.resourceTypeNotSupportedByImplementation(index, resourceTypeName));
		else
		{
			Optional<Resource> resourceToDelete = search(connection, dao.get(), queryParameters);
			if (resourceToDelete.isPresent())
			{
				authorizationHelper.checkDeleteAllowed(index, connection, identity, resourceToDelete.get());

				deleted = exceptionHandler.handleSqlAndResourceNotFoundException(resourceTypeName,
						() -> deleteWithTransaction(dao.get(), connection, parameterConverter.toUuid(resourceTypeName,
								resourceToDelete.get().getIdElement().getIdPart())));

				this.resourceType = dao.get().getResourceType();
				this.id = resourceToDelete.get().getIdElement().getIdPart();

				setNewIdIfResourceExistsInTranslationTable(idTranslationTable, id);
			}
		}
	}

	private void setNewIdIfResourceExistsInTranslationTable(Map<String, IdType> idTranslationTable, String id)
	{
		idTranslationTable.entrySet().stream().filter(e -> e.getValue().equals(new IdType(resourceTypeName, id)))
				.findFirst().ifPresent(entry -> idTranslationTable.put(entry.getKey(),
						new IdType(resourceTypeName, UUID.randomUUID().toString())));
	}

	protected boolean deleteWithTransaction(ResourceDao<?> dao, Connection connection, UUID uuid)
			throws SQLException, ResourceNotFoundException
	{
		return dao.deleteWithTransaction(connection, uuid);
	}

	private Optional<Resource> search(Connection connection, ResourceDao<?> dao,
			Map<String, List<String>> queryParameters)
	{
		if (Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(queryParameters::containsKey))
		{
			logger.warn(
					"Query contains parameter not applicable in this conditional delete context: '{}', parameters {} will be ignored",
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					Arrays.toString(SearchQuery.STANDARD_PARAMETERS));

			queryParameters = queryParameters.entrySet().stream()
					.filter(e -> !Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(p -> p.equals(e.getKey())))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		}

		SearchQuery<?> query = dao.createSearchQueryWithoutUserFilter(PageAndCount.single());
		query.configureParameters(queryParameters);

		List<SearchQueryParameterError> unsupportedQueryParameters = query.getUnsupportedQueryParameters();
		if (!unsupportedQueryParameters.isEmpty())
			throw new WebApplicationException(responseGenerator.badConditionalDeleteRequest(index,
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					unsupportedQueryParameters));

		PartialResult<?> result = exceptionHandler
				.handleSqlException(() -> dao.searchWithTransaction(connection, query));

		if (result.getTotal() <= 0)
		{
			return Optional.empty();
		}
		else if (result.getTotal() == 1)
		{
			return Optional.of(result.getPartialResult().get(0));
		}
		else // if (result.getOverallCount() > 1)
		{
			throw new WebApplicationException(responseGenerator.badConditionalDeleteRequestMultipleMatches(index,
					resourceTypeName, UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString()));
		}
	}

	@Override
	public Optional<BundleEntryComponent> postExecute(Connection connection, EventHandler eventHandler)
	{
		try
		{
			if (deleted)
				eventHandler.handleEvent(eventGenerator.newResourceDeletedEvent(resourceType, id));
		}
		catch (Exception e)
		{
			logger.debug("Error while handling resource deleted event", e);
			logger.warn("Error while handling resource deleted event: {} - {}", e.getClass().getName(), e.getMessage());
		}

		BundleEntryComponent resultEntry = new BundleEntryComponent();
		BundleEntryResponseComponent response = resultEntry.getResponse();
		if (resourceTypeName != null && id != null)
		{
			response.setStatus(Status.OK.getStatusCode() + " " + Status.OK.getReasonPhrase());
			response.setOutcome(responseGenerator.resourceDeleted(resourceTypeName, id));
		}
		else
			response.setStatus(Status.NO_CONTENT.getStatusCode() + " " + Status.NO_CONTENT.getReasonPhrase());

		return Optional.of(resultEntry);
	}

	@Override
	public String getResourceTypeName()
	{
		return resourceTypeName;
	}
}
