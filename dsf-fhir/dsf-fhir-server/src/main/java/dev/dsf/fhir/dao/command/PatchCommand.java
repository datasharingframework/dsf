/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.hl7.fhir.r4.model.Parameters;
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
import dev.dsf.fhir.dao.jdbc.LargeObjectManager;
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
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.patch.FhirPatchException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.RuntimeDelegate;

/**
 * Applies a FHIRPath Patch ({@link Parameters} body) to an existing resource inside a transaction or batch bundle. The
 * target resource type and id are taken from {@code Bundle.entry.request.url} (as with DELETE); the patch is applied to
 * a copy of the current resource and the result is written through the same code path as an update, so authorization,
 * validation, versioning and the Prefer header behave identically.
 */
public class PatchCommand extends AbstractCommand implements ModifyingCommand
{
	private static final Logger logger = LoggerFactory.getLogger(PatchCommand.class);

	private final Parameters patch;
	private final DaoProvider daoProvider;
	private final ExceptionHandler exceptionHandler;
	private final ParameterConverter parameterConverter;
	private final ResponseGenerator responseGenerator;
	private final ReferenceCleaner referenceCleaner;
	private final EventGenerator eventGenerator;
	private final DefaultProfileProvider defaultProfileProvider;
	private final boolean enableValidation;

	private final String resourceTypeName;

	private ResourceDao<Resource> dao;
	private Resource updatedResource;
	private ValidationResult validationResult;

	public PatchCommand(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper, Parameters patch,
			DaoProvider daoProvider, ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			ResponseGenerator responseGenerator, ReferenceCleaner referenceCleaner, EventGenerator eventGenerator,
			DefaultProfileProvider defaultProfileProvider, boolean enableValidation)
	{
		super(3, index, identity, returnType, bundle, entry, serverBase, authorizationHelper);

		this.patch = patch;
		this.daoProvider = daoProvider;
		this.exceptionHandler = exceptionHandler;
		this.parameterConverter = parameterConverter;
		this.responseGenerator = responseGenerator;
		this.referenceCleaner = referenceCleaner;
		this.eventGenerator = eventGenerator;
		this.defaultProfileProvider = defaultProfileProvider;
		this.enableValidation = enableValidation;

		this.resourceTypeName = parseResourceTypeName(entry);

		if (PreferReturnType.OPERATION_OUTCOME.equals(returnType) && !enableValidation)
			throw new IllegalArgumentException("Return type 'operation outcome' not allowed if validation disabled");
	}

	private static String parseResourceTypeName(BundleEntryComponent entry)
	{
		String url = entry.getRequest().getUrl();
		if (url == null || url.isBlank())
			return null;

		List<String> segments = UriComponentsBuilder.fromUriString(url).build().getPathSegments();
		return segments.isEmpty() ? null : segments.get(0);
	}

	@Override
	public String getResourceTypeName()
	{
		return resourceTypeName;
	}

	@Override
	public void execute(Map<String, IdType> idTranslationTable, LargeObjectManager largeObjectManager,
			Connection connection, ValidationHelper validationHelper) throws SQLException, WebApplicationException
	{
		UriComponents components = UriComponentsBuilder.fromUriString(entry.getRequest().getUrl()).build();
		List<String> pathSegments = components.getPathSegments();

		Resource dbResource;
		if (pathSegments.size() == 2 && components.getQueryParams().isEmpty())
			dbResource = readById(connection, pathSegments.get(0), pathSegments.get(1));
		else if (pathSegments.size() == 1 && !components.getQueryParams().isEmpty())
			dbResource = readByCondition(connection, pathSegments.get(0),
					parameterConverter.urlDecodeQueryParameters(components.getQueryParams()));
		else
			throw new WebApplicationException(responseGenerator
					.badRequestPatch("Invalid patch request url '" + entry.getRequest().getUrl() + "'"));

		Resource patchedResource;
		try
		{
			patchedResource = parameterConverter.applyPatch(dbResource, patch);
		}
		catch (FhirPatchException e)
		{
			throw new WebApplicationException(responseGenerator.badRequestPatch(e.getMessage()));
		}

		// keep the id of the current resource; the patch is applied to a copy, so dbResource stays unmodified and can
		// be used as the 'old' resource for the update authorization check
		patchedResource.setIdElement(dbResource.getIdElement());

		if (enableValidation)
		{
			defaultProfileProvider.setDefaultProfile(patchedResource);
			validationResult = validationHelper.checkResourceValidForUpdate(identity, patchedResource);
		}

		authorizationHelper.checkUpdateAllowed(index, connection, identity, dbResource, patchedResource);

		Optional<Long> ifMatch = Optional.ofNullable(entry.getRequest().getIfMatch())
				.flatMap(parameterConverter::toEntityTag).flatMap(parameterConverter::toVersion);

		updatedResource = exceptionHandler.handleSqlExAndResourceNotFoundExAndResouceVersionNonMatchEx(resourceTypeName,
				() -> dao.updateWithTransaction(largeObjectManager, connection, patchedResource, ifMatch.orElse(null)));
	}

	private Resource readById(Connection connection, String resourceTypeName, String id)
	{
		this.dao = getDao(resourceTypeName);
		UUID uuid = parameterConverter.toUuid(resourceTypeName, id);

		return exceptionHandler
				.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
						() -> dao.readWithTransaction(connection, uuid))
				.orElseThrow(() -> new WebApplicationException(responseGenerator.notFound(id, resourceTypeName)));
	}

	private Resource readByCondition(Connection connection, String resourceTypeName,
			Map<String, List<String>> queryParameters)
	{
		this.dao = getDao(resourceTypeName);

		if (Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(queryParameters::containsKey))
		{
			logger.warn(
					"Query contains parameter not applicable in this conditional patch context: '{}', parameters {} will be ignored",
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					Arrays.toString(SearchQuery.STANDARD_PARAMETERS));

			queryParameters = queryParameters.entrySet().stream()
					.filter(e -> Arrays.stream(SearchQuery.STANDARD_PARAMETERS).noneMatch(p -> p.equals(e.getKey())))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		}

		SearchQuery<?> query = dao.createSearchQueryWithoutUserFilter(PageAndCount.single());
		query.configureParameters(queryParameters);

		List<SearchQueryParameterError> unsupportedQueryParameters = query.getUnsupportedQueryParameters();
		if (!unsupportedQueryParameters.isEmpty())
			throw new WebApplicationException(responseGenerator.badRequest(
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					unsupportedQueryParameters));

		PartialResult<?> result = exceptionHandler
				.handleSqlException(() -> dao.searchWithTransaction(connection, query));

		String queryString = UriComponentsBuilder.newInstance()
				.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString();

		if (result.getTotal() <= 0)
			throw new WebApplicationException(responseGenerator.patchTargetNotFound(resourceTypeName, queryString));
		else if (result.getTotal() == 1)
			return result.getPartialResult().get(0);
		else
			throw new WebApplicationException(responseGenerator.multipleExists(resourceTypeName, queryString));
	}

	private ResourceDao<Resource> getDao(String resourceTypeName)
	{
		Optional<ResourceDao<?>> optDao = daoProvider.getDao(resourceTypeName);

		if (optDao.isEmpty())
			throw new WebApplicationException(
					responseGenerator.resourceTypeNotSupportedByImplementation(index, resourceTypeName));

		@SuppressWarnings("unchecked")
		ResourceDao<Resource> d = (ResourceDao<Resource>) optDao.get();
		return d;
	}

	@Override
	public Optional<BundleEntryComponent> postExecute(Connection connection, EventHandler eventHandler)
	{
		Resource updatedResourceWithResolvedReferences = latestOrErrorIfDeletedOrNotFound(connection, updatedResource);

		referenceCleaner.cleanLiteralReferences(updatedResourceWithResolvedReferences);

		try
		{
			eventHandler.handleEvent(eventGenerator.newResourceUpdatedEvent(updatedResourceWithResolvedReferences));
		}
		catch (Exception e)
		{
			logger.debug("Error while handling resource updated event", e);
			logger.warn("Error while handling resource updated event: {} - {}", e.getClass().getName(), e.getMessage());
		}

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

			if (validationResult != null)
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

	private Resource latestOrErrorIfDeletedOrNotFound(Connection connection, Resource resource)
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
		if ("Binary".equals(resourceTypeName))
			return daoProvider.getDao(resourceTypeName).map(d -> d.createLargeObjectManager(connection))
					.orElse(LargeObjectManager.NO_OP);

		return LargeObjectManager.NO_OP;
	}
}
