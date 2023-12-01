package dev.dsf.fhir.webservice.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.authorization.AuthorizationRuleProvider;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.history.HistoryService;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.parameters.ResourceLastUpdated;
import dev.dsf.fhir.search.parameters.StructureDefinitionUrl;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.SnapshotGenerator.SnapshotWithValidationMessages;
import dev.dsf.fhir.webservice.specification.StructureDefinitionService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriInfo;

public class StructureDefinitionServiceImpl extends
		AbstractResourceServiceImpl<StructureDefinitionDao, StructureDefinition> implements StructureDefinitionService
{
	private static final Logger logger = LoggerFactory.getLogger(StructureDefinitionServiceImpl.class);

	private final StructureDefinitionDao snapshotDao;
	private final SnapshotGenerator snapshotGenerator;

	public StructureDefinitionServiceImpl(String path, String serverBase, int defaultPageCount,
			StructureDefinitionDao dao, ResourceValidator validator, EventHandler eventHandler,
			ExceptionHandler exceptionHandler, EventGenerator eventGenerator, ResponseGenerator responseGenerator,
			ParameterConverter parameterConverter, ReferenceExtractor referenceExtractor,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			AuthorizationRuleProvider authorizationRuleProvider, StructureDefinitionDao structureDefinitionSnapshotDao,
			SnapshotGenerator sanapshotGenerator, HistoryService historyService)
	{
		super(path, StructureDefinition.class, serverBase, defaultPageCount, dao, validator, eventHandler,
				exceptionHandler, eventGenerator, responseGenerator, parameterConverter, referenceExtractor,
				referenceResolver, referenceCleaner, authorizationRuleProvider, historyService);

		this.snapshotDao = structureDefinitionSnapshotDao;
		this.snapshotGenerator = sanapshotGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(snapshotGenerator, "snapshotGenerator");
	}

	@Override
	protected Consumer<StructureDefinition> preCreate(StructureDefinition resource) throws WebApplicationException
	{
		StructureDefinition forPost = resource.hasSnapshot() ? resource.copy() : null;

		resource.setSnapshot(null);

		return postCreate(forPost);
	}

	@Override
	protected Consumer<StructureDefinition> preUpdate(StructureDefinition resource)
	{
		StructureDefinition forPost = resource.hasSnapshot() ? resource.copy() : null;

		resource.setSnapshot(null);

		return postUpdate(forPost);
	}

	private Consumer<StructureDefinition> postCreate(StructureDefinition preResource)
	{
		return postResource ->
		{
			if (preResource != null && preResource.hasSnapshot())
			{
				exceptionHandler.catchAndLogSqlAndResourceNotFoundException(resourceTypeName,
						() -> snapshotDao.createWithId(preResource,
								parameterConverter.toUuid(resourceTypeName, postResource.getIdElement().getIdPart())));
			}
			else if (postResource != null)
			{
				try
				{
					SnapshotWithValidationMessages s = snapshotGenerator.generateSnapshot(postResource);

					if (s != null && s.getSnapshot() != null && s.getMessages().isEmpty())
						exceptionHandler.catchAndLogSqlAndResourceNotFoundException(resourceTypeName,
								() -> snapshotDao.createWithId(postResource, parameterConverter.toUuid(resourceTypeName,
										postResource.getIdElement().getIdPart())));
				}
				catch (Exception e)
				{
					logger.warn("Error while generating snapshot for StructureDefinition with id "
							+ postResource.getIdElement().getIdPart(), e);
				}
			}
		};
	}

	private Consumer<StructureDefinition> postUpdate(StructureDefinition preResource)
	{
		return postResource ->
		{
			if (preResource != null && preResource.hasSnapshot())
			{
				if (postResource != null)
					preResource.setIdElement(postResource.getIdElement().copy());

				exceptionHandler.catchAndLogSqlAndResourceNotFoundException(resourceTypeName,
						() -> snapshotDao.update(preResource));
			}
			else if (postResource != null)
			{
				try
				{
					SnapshotWithValidationMessages s = snapshotGenerator.generateSnapshot(postResource);

					if (s != null && s.getSnapshot() != null && s.getMessages().isEmpty())
						exceptionHandler.catchAndLogSqlAndResourceNotFoundException(resourceTypeName,
								() -> snapshotDao.update(s.getSnapshot()));
				}
				catch (Exception e)
				{
					logger.warn("Error while generating snapshot for StructureDefinition with id "
							+ postResource.getIdElement().getIdPart(), e);
				}
			}
		};
	}

	@Override
	protected Consumer<String> preDelete(String id)
	{
		return this::afterDelete;
	}

	private void afterDelete(String id)
	{
		exceptionHandler.catchAndLogSqlAndResourceNotFoundException(resourceTypeName,
				() -> snapshotDao.delete(parameterConverter.toUuid(resourceTypeName, id)));
	}

	@Override
	public Response postSnapshotNew(String snapshotPath, Parameters parameters, UriInfo uri, HttpHeaders headers)
	{
		ParametersParameterComponent urlType = parameters.getParameter("url");
		Optional<ParametersParameterComponent> resource = parameters.getParameter().stream()
				.filter(p -> "resource".equals(p.getName())).findFirst();

		if (urlType != null && resource.isEmpty())
		{
			if (!(urlType.getValue() instanceof StringType || urlType.getValue() instanceof UriType))
				return Response.status(Status.BAD_REQUEST).build(); // TODO OperationOutcome

			@SuppressWarnings("unchecked")
			PrimitiveType<String> url = (PrimitiveType<String>) urlType.getValue();

			logger.trace("Parameters with url {}", url.getValue());

			return getSnapshot(url.getValue(), uri, headers);
		}
		else if (urlType == null && resource.isPresent() && resource.get().getResource() != null)
		{
			if (!(resource.get().getResource() instanceof StructureDefinition))
				return Response.status(Status.BAD_REQUEST).build(); // TODO OperationOutcome

			StructureDefinition sd = (StructureDefinition) resource.get().getResource();

			logger.trace("Parameters with StructureDefinition resource url {}", sd.getUrl());

			if (!sd.hasDifferential())
				return Response.status(Status.BAD_REQUEST).build(); // TODO OperationOutcome

			if (sd.hasSnapshot())
				return responseGenerator
						.response(Status.OK, sd, parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers))
						.build();
			else
				return responseGenerator.response(Status.OK, generateSnapshot(sd),
						parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers)).build();
		}
		else
		{
			// TODO OperationOutcome resource vs. url
			return Response.status(Status.BAD_REQUEST).build();
		}
	}

	private Response getSnapshot(String url, UriInfo uri, HttpHeaders headers)
	{
		SearchQuery<StructureDefinition> query = snapshotDao.createSearchQuery(getCurrentIdentity(), 1, 1);
		Map<String, List<String>> searchParameters = new HashMap<>();
		searchParameters.put(StructureDefinitionUrl.PARAMETER_NAME, Collections.singletonList(url));
		searchParameters.put(SearchQuery.PARAMETER_SORT,
				Collections.singletonList("-" + ResourceLastUpdated.PARAMETER_NAME));
		query.configureParameters(searchParameters);

		PartialResult<StructureDefinition> result = exceptionHandler
				.handleSqlException(() -> snapshotDao.search(query));

		Optional<StructureDefinition> snapshot = Optional
				.ofNullable(result.getPartialResult().isEmpty() ? null : result.getPartialResult().get(0));

		return snapshot
				.map(d -> responseGenerator.response(Status.OK, d,
						parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers)))
				.orElse(Response.status(Status.NOT_FOUND)).build();
	}

	@Override
	public Response getSnapshotNew(String snapshotPath, UriInfo uri, HttpHeaders headers)
	{
		return getSnapshot(uri.getQueryParameters().getFirst("url"), uri, headers);
	}

	@Override
	public Response postSnapshotExisting(String snapshotPath, String id, UriInfo uri, HttpHeaders headers)
	{
		return getSnapshotExisting(snapshotPath, id, uri, headers);
	}

	@Override
	public Response getSnapshotExisting(String snapshotPath, String id, UriInfo uri, HttpHeaders headers)
	{
		Optional<StructureDefinition> snapshot = exceptionHandler.catchAndLogSqlAndResourceDeletedExceptionAndIfReturn(
				() -> snapshotDao.read(parameterConverter.toUuid(resourceTypeName, id)), Optional::empty,
				Optional::empty);

		if (snapshot.isPresent())
			return snapshot.map(d -> responseGenerator.response(Status.OK, d,
					parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers))).get().build();

		Optional<StructureDefinition> differential = exceptionHandler.handleSqlAndResourceDeletedException(serverBase,
				resourceTypeName, () -> dao.read(parameterConverter.toUuid(resourceTypeName, id)));

		return differential.map(this::generateSnapshot)
				.map(d -> responseGenerator.response(Status.OK, d,
						parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers)))
				.orElse(Response.status(Status.NOT_FOUND)).build();
	}

	private StructureDefinition generateSnapshot(StructureDefinition differential)
	{
		SnapshotWithValidationMessages snapshot = snapshotGenerator.generateSnapshot(differential);

		if (snapshot.getMessages().isEmpty())
			return snapshot.getSnapshot();
		else
		{
			OperationOutcome outcome = new OperationOutcome();
			List<OperationOutcomeIssueComponent> issues = snapshot.getMessages().stream()
					.map(vm -> new OperationOutcomeIssueComponent().setSeverity(IssueSeverity.ERROR)
							.setCode(IssueType.STRUCTURE).setDiagnostics(vm.getMessage()))
					.collect(Collectors.toList());
			outcome.setIssue(issues);
			throw new WebApplicationException(Response.status(Status.INTERNAL_SERVER_ERROR).entity(outcome).build());
		}
	}
}
