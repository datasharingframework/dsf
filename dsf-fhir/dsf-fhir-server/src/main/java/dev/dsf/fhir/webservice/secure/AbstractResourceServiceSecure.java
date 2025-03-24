package dev.dsf.fhir.webservice.secure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponentsBuilder;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.ResourceDao;
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
import dev.dsf.fhir.service.ResourceReference;
import dev.dsf.fhir.service.ResourceReference.ReferenceType;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.BasicResourceService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;
import jakarta.ws.rs.core.UriInfo;

public abstract class AbstractResourceServiceSecure<D extends ResourceDao<R>, R extends Resource, S extends BasicResourceService<R>>
		extends AbstractServiceSecure<S> implements BasicResourceService<R>, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractResourceServiceSecure.class);

	protected final ReferenceCleaner referenceCleaner;
	protected final ReferenceExtractor referenceExtractor;
	protected final Class<R> resourceType;
	protected final String resourceTypeName;
	protected final D dao;
	protected final ExceptionHandler exceptionHandler;
	protected final ParameterConverter parameterConverter;
	protected final AuthorizationRule<R> authorizationRule;
	protected final ResourceValidator resourceValidator;
	protected final ValidationRules validationRules;

	public AbstractResourceServiceSecure(S delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, Class<R> resourceType, D dao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<R> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver);

		this.referenceCleaner = referenceCleaner;
		this.referenceExtractor = referenceExtractor;
		this.resourceType = resourceType;
		this.resourceTypeName = resourceType.getAnnotation(ResourceDef.class).name();
		this.dao = dao;
		this.exceptionHandler = exceptionHandler;
		this.parameterConverter = parameterConverter;
		this.authorizationRule = authorizationRule;
		this.resourceValidator = resourceValidator;
		this.validationRules = validationRules;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		Objects.requireNonNull(referenceExtractor, "referenceExtractor");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(resourceTypeName, "resourceTypeName");
		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		Objects.requireNonNull(parameterConverter, "parameterConverter");
		Objects.requireNonNull(authorizationRule, "authorizationRule");
		Objects.requireNonNull(resourceValidator, "resourceValidator");
		Objects.requireNonNull(validationRules, "validationRules");
	}

	private String toValidationLogMessage(ValidationResult validationResult)
	{
		return validationResult
				.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
						+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage())
				.collect(Collectors.joining(", ", "[", "]"));
	}

	private Response withResourceValidation(R resource, Predicate<R> failValidationOnErrorOrFatal, UriInfo uri,
			HttpHeaders headers, String method, Supplier<Response> delegate)
	{
		ValidationResult validationResult = resourceValidator.validate(resource);

		if (failValidationOnErrorOrFatal.test(resource) && validationResult.getMessages().stream()
				.anyMatch(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
						|| ResultSeverityEnum.FATAL.equals(m.getSeverity())))
		{
			logger.warn("{} of {} unauthorized, resource not valid: {}", method, resource.fhirType(),
					toValidationLogMessage(validationResult));

			OperationOutcome outcome = new OperationOutcome();
			validationResult.populateOperationOutcome(outcome);
			return responseGenerator.response(Status.FORBIDDEN, outcome,
					parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers)).build();
		}
		else
		{
			if (!validationResult.getMessages().isEmpty())
				logger.warn("Resource {} validated with messages: {}{}", resource.fhirType(),
						toValidationLogMessage(validationResult),
						(validationResult.getMessages().stream()
								.anyMatch(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
										|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
												? ", ignoring error and fatal messages"
												: ""));

			return delegate.get();
		}
	}

	@Override
	public Response create(R resource, UriInfo uri, HttpHeaders headers)
	{
		resolveLiteralInternalRelatedArtifactOrAttachmentUrls(resource);

		Optional<String> reasonCreateAllowed = authorizationRule.reasonCreateAllowed(getCurrentIdentity(), resource);

		if (reasonCreateAllowed.isEmpty())
		{
			audit.info("Create of resource {} denied for user '{}'", resourceTypeName, getCurrentIdentity().getName());
			return forbidden("create");
		}
		else
		{
			return withResourceValidation(resource, validationRules::failOnErrorOrFatalBeforeCreate, uri, headers,
					"Create", () ->
					{
						audit.info("Create of resource {} allowed for user '{}', reason: {}", resourceTypeName,
								getCurrentIdentity().getName(), reasonCreateAllowed.get());

						Response created = logResultStatus(() ->
						{
							Response response = delegate.create(resource, uri, headers);
							return response;
						}, status -> audit.info("Create of resource {} for user '{}' successful, status: {} {}",
								resourceTypeName, getCurrentIdentity().getName(), status.getStatusCode(),
								status.getReasonPhrase()),
								status -> audit.info("Create of resource {} for user '{}' failed, status: {} {}",
										resourceTypeName, getCurrentIdentity().getName(), status.getStatusCode(),
										status.getReasonPhrase()));

						if (created.hasEntity() && !resourceType.isInstance(created.getEntity())
								&& !(created.getEntity() instanceof OperationOutcome))
							logger.warn("Create returned with entity of type {}",
									created.getEntity().getClass().getName());
						else if (!created.hasEntity()
								&& !PreferReturnType.MINIMAL.equals(parameterConverter.getPreferReturn(headers)))
							logger.warn("Create returned with status {} {}, but no entity",
									created.getStatusInfo().getStatusCode(), created.getStatusInfo().getReasonPhrase());

						return created;
					});
		}
	}

	private void resolveLiteralInternalRelatedArtifactOrAttachmentUrls(R resource)
	{
		if (resource == null)
			return;

		referenceExtractor.getReferences(resource)
				.filter(ref -> ReferenceType.RELATED_ARTEFACT_LITERAL_INTERNAL_URL.equals(ref.getType(serverBase))
						|| ReferenceType.ATTACHMENT_LITERAL_INTERNAL_URL.equals(ref.getType(serverBase)))
				.forEach(this::resolveLiteralInternalRelatedArtifactOrAttachmentUrl);
	}

	private void resolveLiteralInternalRelatedArtifactOrAttachmentUrl(ResourceReference reference)
	{
		if (reference.hasRelatedArtifact() || reference.hasAttachment())
		{
			IdType newId = new IdType(reference.getValue());
			String absoluteUrl = newId.withServerBase(serverBase, newId.getResourceType()).getValue();

			if (reference.hasRelatedArtifact())
				reference.getRelatedArtifact().setUrl(absoluteUrl);
			else if (reference.hasAttachment())
				reference.getAttachment().setUrl(absoluteUrl);
		}
	}

	@Override
	public Response read(String id, UriInfo uri, HttpHeaders headers)
	{
		Response read = delegate.read(id, uri, headers);

		if (read.hasEntity() && resourceType.isInstance(read.getEntity()))
		{
			final R entity = resourceType.cast(read.getEntity());
			final String entityId = entity.getIdElement().getIdPart();
			final long entityVersion = entity.getIdElement().getVersionIdPartAsLong();
			final Optional<String> reasonReadAllowed = authorizationRule.reasonReadAllowed(getCurrentIdentity(),
					entity);

			if (reasonReadAllowed.isEmpty())
			{
				audit.info("Read of {}/{}/_history/{} denied for identity '{}'", resourceTypeName, entityId,
						entityVersion, getCurrentIdentity().getName());
				return forbidden("read");
			}
			else
			{
				audit.info("Read of {}/{}/_history/{} allowed for identity '{}', reason: {}", resourceTypeName,
						entityId, entityVersion, getCurrentIdentity().getName(), reasonReadAllowed.get());
				return logResultStatus(() ->
				{
					// if not modified remove entity
					if (Status.NOT_MODIFIED.getStatusCode() == read.getStatus())
						return Response.notModified(read.getEntityTag()).lastModified(entity.getMeta().getLastUpdated())
								.build();
					else
						return read;
				}, status -> audit.info("Read of {}/{}/_history/{} for identity '{}' successful, status: {} {}",
						resourceTypeName, entityId, entityVersion, getCurrentIdentity().getName(),
						read.getStatusInfo().getStatusCode(), read.getStatusInfo().getReasonPhrase()),
						status -> audit.info("Read of {}/{}/_history/{} for identity '{}' failed, status: {} {}",
								resourceTypeName, entityId, entityVersion, getCurrentIdentity().getName(),
								read.getStatusInfo().getStatusCode(), read.getStatusInfo().getReasonPhrase()));
			}
		}
		else if (read.hasEntity() && read.getEntity() instanceof OperationOutcome)
		{
			audit.info("Read of {} for identity '{}' returned with OperationOutcome, status {} {}", resourceTypeName,
					getCurrentIdentity().getName(), read.getStatusInfo().getStatusCode(),
					read.getStatusInfo().getReasonPhrase());

			logger.info("Returning with OperationOutcome, status {} {}", read.getStatusInfo().getStatusCode(),
					read.getStatusInfo().getReasonPhrase());
			return read;
		}
		else if (read.hasEntity())
		{
			audit.info("Read of {} denied for identity '{}', not a {}", resourceTypeName,
					getCurrentIdentity().getName(), resourceTypeName);
			return forbidden("read");
		}
		else
		{
			audit.info("Read of {} for identity '{}' returned without entity, status {} {}", resourceTypeName,
					getCurrentIdentity().getName(), read.getStatusInfo().getStatusCode(),
					read.getStatusInfo().getReasonPhrase());

			logger.info("Returning with status {} {}, but no entity", read.getStatusInfo().getStatusCode(),
					read.getStatusInfo().getReasonPhrase());
			return read;
		}
	}

	@Override
	public Response vread(String id, long version, UriInfo uri, HttpHeaders headers)
	{
		Response read = delegate.vread(id, version, uri, headers);

		if (read.hasEntity() && resourceType.isInstance(read.getEntity()))
		{
			final R entity = resourceType.cast(read.getEntity());
			final String entityId = entity.getIdElement().getIdPart();
			final long entityVersion = entity.getIdElement().getVersionIdPartAsLong();
			final Optional<String> reasonReadAllowed = authorizationRule.reasonReadAllowed(getCurrentIdentity(),
					entity);

			if (reasonReadAllowed.isEmpty())
			{
				audit.info("Read of {}/{}/_history/{} denied for identity '{}'", resourceTypeName, entityId,
						entityVersion, getCurrentIdentity().getName());
				return forbidden("read");
			}
			else
			{
				audit.info("Read of {}/{}/_history/{} allowed for identity '{}', reason: {}", resourceTypeName,
						entityId, entityVersion, getCurrentIdentity().getName(), reasonReadAllowed.get());
				return logResultStatus(() ->
				{
					// if not modified remove entity
					if (Status.NOT_MODIFIED.getStatusCode() == read.getStatus())
						return Response.notModified(read.getEntityTag()).lastModified(entity.getMeta().getLastUpdated())
								.build();
					else
						return read;
				}, status -> audit.info("Read of {}/{}/_history/{} for identity '{}' successful, status: {} {}",
						resourceTypeName, entityId, entityVersion, getCurrentIdentity().getName(),
						read.getStatusInfo().getStatusCode(), read.getStatusInfo().getReasonPhrase()),
						status -> audit.info("Read of {}/{}/_history/{} for identity '{}' failed, status: {} {}",
								resourceTypeName, entityId, entityVersion, getCurrentIdentity().getName(),
								read.getStatusInfo().getStatusCode(), read.getStatusInfo().getReasonPhrase()));
			}
		}
		else if (read.hasEntity() && read.getEntity() instanceof OperationOutcome)
		{
			audit.info("Read of {} for identity '{}' returned with OperationOutcome, status: {} {}", resourceTypeName,
					getCurrentIdentity().getName(), read.getStatusInfo().getStatusCode(),
					read.getStatusInfo().getReasonPhrase());

			logger.info("Returning with OperationOutcome, status {} {}", read.getStatusInfo().getStatusCode(),
					read.getStatusInfo().getReasonPhrase());
			return read;
		}
		else if (read.hasEntity())
		{
			audit.info("Read of {} denied for identity '{}', not a {}", resourceTypeName,
					getCurrentIdentity().getName(), resourceTypeName);
			return forbidden("read");
		}
		else
		{
			audit.info("Read of {} for identity '{}' returned without entity, status: {} {}", resourceTypeName,
					getCurrentIdentity().getName(), read.getStatusInfo().getStatusCode(),
					read.getStatusInfo().getReasonPhrase());

			logger.info("Returning with status {} {}, but no entity", read.getStatusInfo().getStatusCode(),
					read.getStatusInfo().getReasonPhrase());
			return read;
		}
	}

	@Override
	public Response history(UriInfo uri, HttpHeaders headers)
	{
		Optional<String> reasonHistoryAllowed = authorizationRule.reasonHistoryAllowed(getCurrentIdentity());
		if (reasonHistoryAllowed.isEmpty())
		{
			audit.info("History of {} denied for identity '{}'", resourceTypeName, getCurrentIdentity().getName());
			return forbidden("history");
		}
		else
		{
			audit.info("History of {} allowed for identity '{}', reason: {}", resourceTypeName,
					getCurrentIdentity().getName(), reasonHistoryAllowed.get());
			return logResultStatus(() ->
			{
				Response response = delegate.history(uri, headers);
				return response;
			}, status -> audit.info("History of {} for identity '{}' successful: {} {}", resourceTypeName,
					getCurrentIdentity().getName(), status.getStatusCode(), status.getReasonPhrase()),
					status -> audit.info("History of {} for identity '{}' failed: {} {}", resourceTypeName,
							getCurrentIdentity().getName(), status.getStatusCode(), status.getReasonPhrase()));
		}
	}

	@Override
	public Response history(String id, UriInfo uri, HttpHeaders headers)
	{
		Optional<String> reasonHistoryAllowed = authorizationRule.reasonHistoryAllowed(getCurrentIdentity());
		if (reasonHistoryAllowed.isEmpty())
		{
			audit.info("History of {} denied for identity '{}'", resourceTypeName, getCurrentIdentity().getName());
			return forbidden("history");
		}
		else
		{
			audit.info("History of {} allowed for identity '{}', reason: {}", resourceTypeName,
					getCurrentIdentity().getName(), reasonHistoryAllowed.get());
			return logResultStatus(() ->
			{
				Response response = delegate.history(id, uri, headers);
				return response;
			}, status -> audit.info("History of {} for identity '{}' successful: {} {}", resourceTypeName,
					getCurrentIdentity().getName(), status.getStatusCode(), status.getReasonPhrase()),
					status -> audit.info("History of {} for identity '{}' failed: {} {}", resourceTypeName,
							getCurrentIdentity().getName(), status.getStatusCode(), status.getReasonPhrase()));
		}
	}

	@Override
	public Response update(String id, R resource, UriInfo uri, HttpHeaders headers)
	{
		Optional<R> dbResource = exceptionHandler.handleSqlAndResourceDeletedException(serverBase, resourceTypeName,
				() -> dao.read(parameterConverter.toUuid(resourceTypeName, id)));

		if (dbResource.isEmpty())
		{
			audit.info("Create as update of non existing {} denied for identity '{}'", resourceTypeName,
					getCurrentIdentity().getName());
			return responseGenerator.updateAsCreateNotAllowed(resourceTypeName);
		}
		else
		{
			R cleanedDbResource = referenceCleaner.cleanLiteralReferences(dbResource.get());
			return update(id, resource, uri, headers, cleanedDbResource);
		}
	}

	private Response update(String id, R newResource, UriInfo uri, HttpHeaders headers, R oldResource)
	{
		resolveLiteralInternalRelatedArtifactOrAttachmentUrls(newResource);

		final String resourceId = oldResource.getIdElement().getIdPart();
		final long resourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();
		final Optional<String> reasonUpdateAllowed = authorizationRule.reasonUpdateAllowed(getCurrentIdentity(),
				oldResource, newResource);

		if (reasonUpdateAllowed.isEmpty())
		{
			audit.info("Update of {}/{}/_history/{} denied for identity '{}'", resourceTypeName, resourceId,
					resourceVersion, getCurrentIdentity().getName());
			return forbidden("update");
		}
		else
		{
			return withResourceValidation(newResource, validationRules::failOnErrorOrFatalBeforeUpdate, uri, headers,
					"Update", () ->
					{
						audit.info("Update of {}/{}/_history/{} allowed for identity '{}', reason: {}",
								resourceTypeName, resourceId, resourceVersion, getCurrentIdentity().getName(),
								reasonUpdateAllowed.get());
						Response updated = logResultStatus(() ->
						{
							Response response = delegate.update(id, newResource, uri, headers);
							return response;
						}, status -> audit.info(
								"Update of {}/{}/_history/{} for identity '{}' successful, status: {} {}",
								resourceTypeName, resourceId, resourceVersion, getCurrentIdentity().getName(),
								status.getStatusCode(), status.getReasonPhrase()),
								status -> audit.info(
										"Update of {}/{}/_history/{} for identity '{}' failed, status: {} {}",
										resourceTypeName, resourceId, resourceVersion, getCurrentIdentity().getName(),
										status.getStatusCode(), status.getReasonPhrase()));

						if (updated.hasEntity() && !resourceType.isInstance(updated.getEntity())
								&& !(updated.getEntity() instanceof OperationOutcome))
							logger.warn("Update returned with entity of type {}",
									updated.getEntity().getClass().getName());
						else if (!updated.hasEntity()
								&& !PreferReturnType.MINIMAL.equals(parameterConverter.getPreferReturn(headers)))
							logger.warn("Update returned with status {} {}, but no entity",
									updated.getStatusInfo().getStatusCode(), updated.getStatusInfo().getReasonPhrase());

						return updated;
					});
		}
	}

	@Override
	public Response update(R resource, UriInfo uri, HttpHeaders headers)
	{
		Map<String, List<String>> queryParameters = uri.getQueryParameters();
		PartialResult<R> result = getExisting(queryParameters);

		// No matches, no id provided: The server creates the resource.
		if (result.getTotal() <= 0 && !resource.hasId())
		{
			// more security checks and audit log in create method
			return create(resource, uri, headers);
		}

		// No matches, id provided: The server treats the interaction as an Update as Create interaction (or rejects it,
		// if it does not support Update as Create) -> reject
		else if (result.getTotal() <= 0 && resource.hasId())
		{
			audit.info("Create as update of non existing {} denied for identity '{}'", resourceTypeName,
					getCurrentIdentity().getName());
			return responseGenerator.updateAsCreateNotAllowed(resourceTypeName);
		}

		// One Match, no resource id provided OR (resource id provided and it matches the found resource):
		// The server performs the update against the matching resource
		else if (result.getTotal() == 1)
		{
			R dbResource = result.getPartialResult().get(0);
			IdType dbResourceId = dbResource.getIdElement();

			// update: resource has no id
			if (!resource.hasId())
			{
				resource.setIdElement(dbResourceId);
				// more security checks and audit log in update method
				return update(resource.getIdElement().getIdPart(), resource, uri, headers, resource);
			}

			// update: resource has same id
			else if (resource.hasId()
					&& (!resource.getIdElement().hasBaseUrl()
							|| serverBase.equals(resource.getIdElement().getBaseUrl()))
					&& (!resource.getIdElement().hasResourceType()
							|| resourceTypeName.equals(resource.getIdElement().getResourceType()))
					&& dbResourceId.getIdPart().equals(resource.getIdElement().getIdPart()))
			{
				// more security checks and audit log in update method
				return update(resource.getIdElement().getIdPart(), resource, uri, headers, resource);
			}

			// update resource has different id -> 400 Bad Request
			else
			{
				audit.info("Update of {}/{}/_history/{} denied for identity '{}', new resource has different id",
						resourceTypeName, dbResourceId.getValue(), dbResourceId.getVersionIdPart(),
						getCurrentIdentity().getName());
				return responseGenerator.badRequestIdsNotMatching(
						dbResourceId.withServerBase(serverBase, resourceTypeName),
						resource.getIdElement().hasBaseUrl() && resource.getIdElement().hasResourceType()
								? resource.getIdElement()
								: resource.getIdElement().withServerBase(serverBase, resourceTypeName));
			}
		}

		// Multiple matches: The server returns a 412 Precondition Failed error indicating the client's criteria were
		// not selective enough preferably with an OperationOutcome
		else // if (result.getOverallCount() > 1)
		{
			audit.info(
					"Update of {} denied for identity '{}', conditional update criteria not selective enough, multiple matches",
					resourceTypeName, getCurrentIdentity().getName());
			return responseGenerator.multipleExists(resourceTypeName, UriComponentsBuilder.newInstance()
					.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString());
		}
	}

	private PartialResult<R> getExisting(Map<String, List<String>> queryParameters)
	{
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

		List<SearchQueryParameterError> unsupportedQueryParameters = query.getUnsupportedQueryParameters();
		if (!unsupportedQueryParameters.isEmpty())
		{
			audit.info(
					"Update of resource {} denied for identity '{}', conditional update criteria contains unsupported parameters",
					resourceTypeName, getCurrentIdentity().getName());

			Response response = responseGenerator.badRequest(
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					unsupportedQueryParameters);
			throw new WebApplicationException(response);
		}

		return exceptionHandler.handleSqlException(() -> dao.search(query));
	}

	@Override
	public Response delete(String id, UriInfo uri, HttpHeaders headers)
	{
		Optional<R> dbResource = exceptionHandler
				.handleSqlException(() -> dao.readIncludingDeleted(parameterConverter.toUuid(resourceTypeName, id)));

		if (dbResource.isPresent())
		{
			R oldResource = dbResource.get();

			final String resourceId = oldResource.getIdElement().getIdPart();
			final long resourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();
			final Optional<String> reasonDeleteAllowed = authorizationRule.reasonDeleteAllowed(getCurrentIdentity(),
					oldResource);

			if (reasonDeleteAllowed.isEmpty())
			{
				audit.info("Delete of {}/{}/_history/{} denied for identity '{}'", resourceTypeName, resourceId,
						resourceVersion, getCurrentIdentity().getName());
				return forbidden("delete");
			}
			else
			{
				audit.info("Delete of {}/{}/_history/{} allowed for identity '{}', reason: {}", resourceTypeName,
						resourceId, resourceVersion, getCurrentIdentity().getName(), reasonDeleteAllowed.get());
				return logResultStatus(() ->
				{
					Response response = delegate.delete(id, uri, headers);
					return response;
				}, status -> audit.info("Delete of {}/{}/_history/{} for identity '{}' successful, status: {} {}",
						resourceTypeName, resourceId, resourceVersion, getCurrentIdentity().getName(),
						status.getStatusCode(), status.getReasonPhrase()),
						status -> audit.info("Delete of {}/{}/_history/{} for identity '{}' failed, status: {} {}",
								resourceTypeName, resourceId, resourceVersion, getCurrentIdentity().getName(),
								status.getStatusCode(), status.getReasonPhrase()));
			}
		}
		else
		{
			audit.info("{} to delete not found for user '{}'", resourceTypeName, getCurrentIdentity().getName());
			return responseGenerator.notFound(id, resourceTypeName);
		}
	}

	@Override
	public Response delete(UriInfo uri, HttpHeaders headers)
	{
		Map<String, List<String>> queryParameters = uri.getQueryParameters();
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

		SearchQuery<R> query = dao.createSearchQuery(getCurrentIdentity(), PageAndCount.single());
		query.configureParameters(queryParameters);

		List<SearchQueryParameterError> unsupportedQueryParameters = query.getUnsupportedQueryParameters();
		if (!unsupportedQueryParameters.isEmpty())
		{
			audit.info(
					"Delete of {} denied for identity '{}', conditional delete criteria contains unsupported parameters",
					resourceTypeName, getCurrentIdentity().getName());
			return responseGenerator.badRequest(
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					unsupportedQueryParameters);
		}

		PartialResult<R> result = exceptionHandler.handleSqlException(() -> dao.search(query));

		// No matches
		if (result.getTotal() <= 0)
		{
			audit.info("No {} resource deleted for identity '{}', conditional delete criteria produced no matches",
					resourceTypeName, getCurrentIdentity().getName());
			return Response.noContent().build(); // TODO return OperationOutcome
		}

		// One Match: The server performs an ordinary delete on the matching resource
		else if (result.getTotal() == 1)
		{
			R resource = result.getPartialResult().get(0);

			// more security checks and audit log in delete method
			return delete(resource.getIdElement().getIdPart(), uri, headers);
		}

		// Multiple matches: A server may choose to delete all the matching resources, or it may choose to return a 412
		// Precondition Failed error indicating the client's criteria were not selective enough.
		else
		{
			audit.info(
					"Delete of {} denied for identity '{}', conditional delete criteria not selective enough, multiple matches",
					resourceTypeName, getCurrentIdentity().getName());
			return responseGenerator.multipleExists(resourceTypeName, UriComponentsBuilder.newInstance()
					.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString());
		}
	}

	@Override
	public Response search(UriInfo uri, HttpHeaders headers)
	{
		Optional<String> reasonSearchAllowed = authorizationRule.reasonSearchAllowed(getCurrentIdentity());
		if (reasonSearchAllowed.isEmpty())
		{
			audit.info("Search of {} denied for identity '{}'", resourceTypeName, getCurrentIdentity().getName());
			return forbidden("search");
		}
		else
		{
			audit.info("Search of {} allowed for identity '{}', reason: {}", resourceTypeName,
					getCurrentIdentity().getName(), reasonSearchAllowed.get());
			return logResultStatus(() ->
			{
				Response response = delegate.search(uri, headers);
				return response;
			}, status -> audit.info("Search of {} for identity '{} successful, status: {} {}'", resourceTypeName,
					getCurrentIdentity().getName(), status.getStatusCode(), status.getReasonPhrase()),
					status -> audit.info("Search of {} for identity '{}' failed, status: {} {}", resourceTypeName,
							getCurrentIdentity().getName(), status.getStatusCode(), status.getReasonPhrase()));
		}
	}

	@Override
	public Response deletePermanently(String deletePath, String id, UriInfo uri, HttpHeaders headers)
	{
		Optional<R> dbResource = exceptionHandler
				.handleSqlException(() -> dao.readIncludingDeleted(parameterConverter.toUuid(resourceTypeName, id)));

		if (dbResource.isPresent())
		{
			final R oldResource = dbResource.get();
			final String resourceId = oldResource.getIdElement().getIdPart();
			final long resourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();
			final Optional<String> reasonDeleteAllowed = authorizationRule
					.reasonPermanentDeleteAllowed(getCurrentIdentity(), oldResource);
			if (reasonDeleteAllowed.isEmpty())
			{
				audit.info("Permanent delete of {}/{}/_history/{} denied for identity '{}'", resourceTypeName,
						resourceId, resourceVersion, getCurrentIdentity().getName());
				return forbidden("delete");
			}
			else
			{
				audit.info("Permanent delete of {}/{}/_history/{} allowed for identity '{}', reason: {}",
						resourceTypeName, resourceId, resourceVersion, getCurrentIdentity().getName(),
						reasonDeleteAllowed.get());

				return logResultStatus(() ->
				{
					Response response = delegate.deletePermanently(deletePath, id, uri, headers);
					return response;
				}, status -> audit.info(
						"Permanent delete of {}/{}/_history/{} by identity '{}' successful, status: {} {}",
						resourceTypeName, resourceId, resourceVersion, getCurrentIdentity().getName(),
						status.getStatusCode(), status.getReasonPhrase()),
						status -> audit.info(
								"Permanent delete of {}/{}/_history/{} by identity '{}' failed, status: {} {}",
								resourceTypeName, resourceId, resourceVersion, getCurrentIdentity().getName(),
								status.getStatusCode(), status.getReasonPhrase()));
			}
		}
		else
		{
			audit.info("{} to permanently delete not found for user '{}'", resourceTypeName,
					getCurrentIdentity().getName());
			return responseGenerator.notFound(id, resourceTypeName);
		}
	}

	private Response logResultStatus(Supplier<Response> responseSupplier, Consumer<StatusType> logSuccessForStatusCode,
			Consumer<StatusType> logErrorForStatusCode)
	{
		try
		{
			Response response = responseSupplier.get();

			if (Family.SUCCESSFUL.equals(response.getStatusInfo().getFamily()))
				logSuccessForStatusCode.accept(response.getStatusInfo());
			else
				logErrorForStatusCode.accept(response.getStatusInfo());

			return response;
		}
		catch (Exception e)
		{
			StatusType status = e instanceof WebApplicationException w && w.getResponse() != null
					? w.getResponse().getStatusInfo()
					: Status.INTERNAL_SERVER_ERROR;

			logErrorForStatusCode.accept(status);

			throw e;
		}
	}
}
