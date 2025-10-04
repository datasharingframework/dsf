package dev.dsf.fhir.service;

import java.sql.Connection;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import dev.dsf.fhir.client.ClientProvider;
import dev.dsf.fhir.client.FhirWebserviceClient;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.service.ResourceReference.ReferenceType;

public class ReferenceResolverImpl implements ReferenceResolver, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ReferenceResolverImpl.class);

	private static final String QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE = "QuestionnaireResponse.questionnaire";
	private static final String TASK_INSTANTIATES_CANONICAL = "Task.instantiatesCanonical";

	private final String serverBase;
	private final DaoProvider daoProvider;
	private final ResponseGenerator responseGenerator;
	private final ExceptionHandler exceptionHandler;
	private final ClientProvider clientProvider;
	private final ParameterConverter parameterConverter;

	public ReferenceResolverImpl(String serverBase, DaoProvider daoProvider, ResponseGenerator responseGenerator,
			ExceptionHandler exceptionHandler, ClientProvider clientProvider, ParameterConverter parameterConverter)
	{
		this.serverBase = serverBase;
		this.daoProvider = daoProvider;
		this.responseGenerator = responseGenerator;
		this.exceptionHandler = exceptionHandler;
		this.clientProvider = clientProvider;
		this.parameterConverter = parameterConverter;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(serverBase, "serverBase");
		Objects.requireNonNull(daoProvider, "daoProvider");
		Objects.requireNonNull(responseGenerator, "responseGenerator");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		Objects.requireNonNull(clientProvider, "clientProvider");
		Objects.requireNonNull(parameterConverter, "parameterConverter");
	}

	@Override
	public boolean referenceCanBeResolved(ResourceReference reference, Connection connection)
	{
		Objects.requireNonNull(reference, "reference");
		Objects.requireNonNull(connection, "connection");

		return switch (reference.getType(serverBase))
		{
			case LITERAL_EXTERNAL, RELATED_ARTEFACT_LITERAL_EXTERNAL_URL, ATTACHMENT_LITERAL_EXTERNAL_URL ->
				clientProvider.endpointExists(reference.getServerBase(serverBase));

			case LOGICAL -> exceptionHandler.handleSqlException(
					() -> daoProvider.getNamingSystemDao().existsWithUniqueIdUriEntryResolvable(connection,
							reference.getReference().getIdentifier().getSystem()));

			case CANONICAL -> QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE.equals(reference.getLocation())
					|| TASK_INSTANTIATES_CANONICAL.equals(reference.getLocation());

			default -> true;
		};
	}

	@Override
	public Optional<Resource> resolveReference(ResourceReference reference, Connection connection)
	{
		Objects.requireNonNull(reference, "reference");
		Objects.requireNonNull(connection, "connection");

		ReferenceType type = reference.getType(serverBase);
		return switch (type)
		{
			case LITERAL_INTERNAL -> resolveLiteralInternalReference(reference, connection);
			case LITERAL_EXTERNAL, RELATED_ARTEFACT_LITERAL_EXTERNAL_URL, ATTACHMENT_LITERAL_EXTERNAL_URL ->
				resolveLiteralExternalReference(reference);
			case CONDITIONAL, RELATED_ARTEFACT_CONDITIONAL_URL, ATTACHMENT_CONDITIONAL_URL ->
				resolveConditionalReference(reference, connection);
			case LOGICAL -> resolveLogicalReference(reference, connection);

			default -> throw new IllegalArgumentException("Reference of type " + type + " not supported");
		};
	}

	private void throwIfReferenceTypeUnexpected(ReferenceType type, ReferenceType expected)
	{
		if (!expected.equals(type))
			throw new IllegalArgumentException("ReferenceType " + expected + " expected, but was " + type);
	}

	private void throwIfReferenceTypeUnexpected(ReferenceType type, EnumSet<ReferenceType> expected)
	{
		if (!expected.contains(type))
			throw new IllegalArgumentException("ReferenceTypes " + expected + " expected, but was " + type);
	}

	private Optional<Resource> resolveLiteralInternalReference(ResourceReference reference, Connection connection)
	{
		Objects.requireNonNull(reference, "reference");
		throwIfReferenceTypeUnexpected(reference.getType(serverBase), ReferenceType.LITERAL_INTERNAL);

		IdType id = new IdType(reference.getReference().getReference());
		Optional<ResourceDao<?>> referenceDao = daoProvider.getDao(id.getResourceType());

		if (referenceDao.isEmpty())
		{
			logger.warn("Reference target type of reference at {} not supported by this implementation",
					reference.getLocation());
			return Optional.empty();
		}
		else
		{
			@SuppressWarnings("unchecked")
			ResourceDao<Resource> d = (ResourceDao<Resource>) referenceDao.get();
			if (!reference.supportsType(d.getResourceType()))
			{
				logger.warn("Reference target type of reference at {} not supported", reference.getLocation());
				return Optional.empty();
			}

			Optional<UUID> uuid = parameterConverter.toUuid(id.getIdPart());

			if (!id.hasVersionIdPart())
				return uuid.flatMap(i -> exceptionHandler.catchAndLogSqlAndResourceDeletedExceptionAndIfReturn(() ->
				{
					if (connection == null)
						return d.read(i);
					else
						return d.readWithTransaction(connection, i);
				}, Optional::empty, Optional::empty));
			else
				return uuid.flatMap(i -> exceptionHandler.catchAndLogSqlAndResourceDeletedExceptionAndIfReturn(() ->
				{
					if (connection == null)
						return d.readVersion(i, id.getVersionIdPartAsLong());
					else
						return d.readVersionWithTransaction(connection, i, id.getVersionIdPartAsLong());
				}, Optional::empty, Optional::empty));
		}
	}

	private Optional<Resource> resolveLiteralExternalReference(ResourceReference reference)
	{
		Objects.requireNonNull(reference, "reference");
		throwIfReferenceTypeUnexpected(reference.getType(serverBase), EnumSet.of(ReferenceType.LITERAL_EXTERNAL,
				ReferenceType.RELATED_ARTEFACT_LITERAL_EXTERNAL_URL, ReferenceType.ATTACHMENT_LITERAL_EXTERNAL_URL));

		String remoteServerBase = reference.getServerBase(serverBase);
		Optional<FhirWebserviceClient> client = clientProvider.getClient(remoteServerBase);

		if (client.isEmpty())
		{
			logger.warn("Literal external reference {} could not be resolved, no remote client for server base {}",
					reference.getReference().getReference(), remoteServerBase);
			return Optional.empty();
		}
		else
		{
			IdType referenceId = new IdType(reference.getReference().getReference());
			logger.debug("Trying to resolve literal external reference {}, at remote server {}",
					reference.getReference().getReference(), remoteServerBase);

			try
			{
				if (!referenceId.hasVersionIdPart())
					return Optional
							.ofNullable(client.get().read(referenceId.getResourceType(), referenceId.getIdPart()));
				else
					return Optional.ofNullable(client.get().read(referenceId.getResourceType(), referenceId.getIdPart(),
							referenceId.getVersionIdPart()));
			}
			catch (Exception e)
			{
				logger.debug("Literal external reference {} could not be resolved on remote server {}",
						reference.getReference().getReference(), remoteServerBase, e);
				logger.error("Literal external reference {} could not be resolved on remote server {}: {} - {}",
						reference.getReference().getReference(), remoteServerBase, e.getClass().getName(),
						e.getMessage());

				return Optional.empty();
			}
		}
	}

	private Optional<Resource> resolveConditionalReference(ResourceReference reference, Connection connection)
	{
		Objects.requireNonNull(reference, "reference");

		ReferenceType referenceType = reference.getType(serverBase);
		throwIfReferenceTypeUnexpected(referenceType, EnumSet.of(ReferenceType.CONDITIONAL,
				ReferenceType.RELATED_ARTEFACT_CONDITIONAL_URL, ReferenceType.ATTACHMENT_CONDITIONAL_URL));

		String referenceValue = reference.getValue();
		String referenceLocation = reference.getLocation();

		UriComponents condition = UriComponentsBuilder.fromUriString(referenceValue).build();
		String path = condition.getPath();
		if (path == null || path.isBlank())
		{
			logger.warn("Bad conditional reference target '{}' of reference at {}", referenceValue, referenceLocation);
			return Optional.empty();
		}

		Optional<ResourceDao<?>> referenceDao = daoProvider.getDao(path);

		if (referenceDao.isEmpty())
		{
			logger.warn("Reference target type of reference at {} not supported by this implementation",
					referenceLocation);
			return Optional.empty();
		}
		else
		{
			ResourceDao<?> d = referenceDao.get();
			if (!reference.supportsType(d.getResourceType()))
			{
				logger.warn("Reference target type of reference at {} not supported", referenceLocation);
				return Optional.empty();
			}

			return search(connection, d, reference, condition.getQueryParams(), referenceType);
		}
	}

	private Optional<Resource> resolveLogicalReference(ResourceReference reference, Connection connection)
	{
		Objects.requireNonNull(reference, "reference");
		throwIfReferenceTypeUnexpected(reference.getType(serverBase), ReferenceType.LOGICAL);

		String targetType = reference.getReference().getType();

		Optional<ResourceDao<?>> referenceDao = daoProvider.getDao(targetType);

		if (referenceDao.isEmpty())
		{
			logger.warn("Reference target type of reference at {} not supported by this implementation",
					reference.getLocation());
			return Optional.empty();
		}
		else
		{
			ResourceDao<?> d = referenceDao.get();
			if (!reference.supportsType(d.getResourceType()))
			{
				logger.warn("Reference target type of reference at {} not supported by this implementation",
						reference.getLocation());
				return Optional.empty();
			}

			Identifier targetIdentifier = reference.getReference().getIdentifier();
			return search(connection, d, reference,
					Map.of("identifier", List.of(targetIdentifier.getSystem() + "|" + targetIdentifier.getValue())),
					ReferenceType.LOGICAL);
		}
	}

	private Optional<Resource> search(Connection connection, ResourceDao<?> referenceTargetDao,
			ResourceReference resourceReference, Map<String, List<String>> queryParameters, ReferenceType referenceType)
	{
		if (Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(queryParameters::containsKey))
		{
			logger.warn(
					"Query contains parameter not applicable in this resolve reference context: '{}', parameters {} will be ignored",
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					Arrays.toString(SearchQuery.STANDARD_PARAMETERS));

			queryParameters = queryParameters.entrySet().stream()
					.filter(e -> !Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(p -> p.equals(e.getKey())))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		}

		SearchQuery<?> query = referenceTargetDao.createSearchQueryWithoutUserFilter(PageAndCount.single());
		query.configureParameters(queryParameters);

		List<SearchQueryParameterError> unsupportedQueryParameters = query.getUnsupportedQueryParameters();
		if (!unsupportedQueryParameters.isEmpty())
		{
			String unsupportedQueryParametersString = unsupportedQueryParameters.stream()
					.map(SearchQueryParameterError::toString).collect(Collectors.joining("; "));

			if (EnumSet.of(ReferenceType.CONDITIONAL, ReferenceType.RELATED_ARTEFACT_CONDITIONAL_URL,
					ReferenceType.ATTACHMENT_CONDITIONAL_URL).contains(referenceType))
			{
				logger.warn("Conditional reference {} at {} in resource contains unsupported queryparameter{} {}",
						queryParameters, resourceReference.getLocation(),
						unsupportedQueryParameters.size() != 1 ? "s" : "", unsupportedQueryParametersString);
				return Optional.empty();
			}
			else
				throw new IllegalStateException("Unable to search for " + referenceTargetDao.getResourceTypeName()
						+ ": Unsupported query parameters");
		}

		PartialResult<?> result = exceptionHandler.handleSqlException(() ->
		{
			if (connection == null)
				return referenceTargetDao.search(query);
			else
				return referenceTargetDao.searchWithTransaction(connection, query);
		});

		if (result.getTotal() == 1)
			return Optional.of(result.getPartialResult().get(0));

		else
		{
			int overallCount = result.getTotal();

			if (ReferenceType.LOGICAL.equals(referenceType))
				logger.warn("Found {} matches for reference at {} with identifier '{}|{}'", overallCount,
						resourceReference.getLocation(), resourceReference.getReference().getIdentifier().getSystem(),
						resourceReference.getReference().getIdentifier().getValue());
			else if (EnumSet.of(ReferenceType.CONDITIONAL, ReferenceType.RELATED_ARTEFACT_CONDITIONAL_URL,
					ReferenceType.ATTACHMENT_CONDITIONAL_URL).contains(referenceType))
				logger.warn("Found {} matches for reference at {} with condition '{}'", overallCount,
						resourceReference.getLocation(),
						UriComponentsBuilder.newInstance().path(referenceTargetDao.getResourceTypeName())
								.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString());
			else if (ReferenceType.CANONICAL.equals(referenceType))
				logger.warn("Found {} matches for reference at {} with url '{}'", overallCount,
						resourceReference.getLocation(), resourceReference.getCanonical().getValue());

			return Optional.empty();
		}
	}

	@Override
	public Optional<OperationOutcome> checkLiteralInternalReference(Resource resource,
			ResourceReference resourceReference, Connection connection) throws IllegalArgumentException
	{
		return checkLiteralInternalReference(resource, resourceReference, connection, null);
	}

	@Override
	public Optional<OperationOutcome> checkLiteralInternalReference(Resource resource, ResourceReference reference,
			Connection connection, Integer bundleIndex) throws IllegalArgumentException
	{
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(reference, "reference");
		Objects.requireNonNull(connection, "connection");
		throwIfReferenceTypeUnexpected(reference.getType(serverBase), EnumSet.of(ReferenceType.LITERAL_INTERNAL,
				ReferenceType.RELATED_ARTEFACT_LITERAL_INTERNAL_URL, ReferenceType.ATTACHMENT_LITERAL_INTERNAL_URL));

		IdType id = new IdType(reference.getValue());
		Optional<ResourceDao<?>> referenceDao = daoProvider.getDao(id.getResourceType());

		if (referenceDao.isEmpty())
			return Optional.of(responseGenerator.referenceTargetTypeNotSupportedByImplementation(bundleIndex, resource,
					reference));
		else
		{
			ResourceDao<?> d = referenceDao.get();
			if (!reference.supportsType(d.getResourceType()))
				return Optional.of(
						responseGenerator.referenceTargetTypeNotSupportedByResource(bundleIndex, resource, reference));

			boolean exists = exceptionHandler.handleSqlException(
					() -> d.existsNotDeletedWithTransaction(connection, id.getIdPart(), id.getVersionIdPart()));
			if (!exists)
				return Optional.of(responseGenerator.referenceTargetNotFoundLocally(bundleIndex, resource, reference));
		}

		return Optional.empty();
	}

	@Override
	public Optional<OperationOutcome> checkLiteralExternalReference(Resource resource,
			ResourceReference resourceReference) throws IllegalArgumentException
	{
		return checkLiteralExternalReference(resource, resourceReference, null);
	}

	@Override
	public Optional<OperationOutcome> checkLiteralExternalReference(Resource resource, ResourceReference reference,
			Integer bundleIndex) throws IllegalArgumentException
	{
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(reference, "reference");
		throwIfReferenceTypeUnexpected(reference.getType(serverBase), EnumSet.of(ReferenceType.LITERAL_EXTERNAL,
				ReferenceType.RELATED_ARTEFACT_LITERAL_EXTERNAL_URL, ReferenceType.ATTACHMENT_LITERAL_EXTERNAL_URL));

		String remoteServerBase = reference.getServerBase(serverBase);
		String referenceValue = reference.getValue();
		Optional<FhirWebserviceClient> client = clientProvider.getClient(remoteServerBase);

		if (client.isEmpty())
		{
			logger.error("Literal external reference {} could not be resolved, no remote client for server base {}",
					referenceValue, remoteServerBase);
			return Optional
					.of(responseGenerator.noEndpointFoundForLiteralExternalReference(bundleIndex, resource, reference));
		}
		else
		{
			IdType referenceId = new IdType(referenceValue);
			logger.debug("Trying to resolve literal external reference {}, at remote server {}", referenceValue,
					remoteServerBase);

			try
			{
				if (client.get().exists(referenceId))
				{
					// resource exists - no error response
					return Optional.empty();
				}
				else
				{
					logger.warn(
							"Literal external reference {} could not be resolved, resource not found on remote server {}",
							referenceValue, remoteServerBase);
					return Optional.of(responseGenerator.referenceTargetNotFoundRemote(bundleIndex, resource, reference,
							remoteServerBase));
				}
			}
			catch (Exception e)
			{
				logger.debug("Literal external reference {} could not be resolved on remote server {}", referenceValue,
						remoteServerBase, e);
				logger.error("Literal external reference {} could not be resolved on remote server {}: {} - {}",
						referenceValue, remoteServerBase, e.getClass().getName(), e.getMessage());

				return Optional.of(responseGenerator.referenceTargetCouldNotBeResolvedOnRemote(bundleIndex, resource,
						reference, remoteServerBase));
			}
		}
	}

	@Override
	public Optional<OperationOutcome> checkConditionalReference(Resource resource, ResourceReference reference,
			Connection connection, Integer bundleIndex) throws IllegalArgumentException
	{
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(reference, "reference");
		Objects.requireNonNull(connection, "connection");
		throwIfReferenceTypeUnexpected(reference.getType(serverBase), ReferenceType.CONDITIONAL);

		UriComponents condition = UriComponentsBuilder.fromUriString(reference.getReference().getReference()).build();
		String path = condition.getPath();
		if (path == null || path.isBlank())
			return Optional.of(responseGenerator.referenceTargetBadCondition(bundleIndex, resource, reference));

		Optional<ResourceDao<?>> referenceDao = daoProvider.getDao(path);

		if (referenceDao.isEmpty())
			return Optional.of(responseGenerator.referenceTargetTypeNotSupportedByImplementation(bundleIndex, resource,
					reference));
		else
		{
			ResourceDao<?> d = referenceDao.get();
			if (!reference.supportsType(d.getResourceType()))
				return Optional.of(
						responseGenerator.referenceTargetTypeNotSupportedByResource(bundleIndex, resource, reference));

			// Resource target =
			return search(resource, bundleIndex, connection, d, reference, condition.getQueryParams(), true);

			// TODO add literal reference for conditional reference somewhere else
			// reference.getReference().setIdentifier(null).setReferenceElement(
			// new IdType(target.getResourceType().name(), target.getIdElement().getIdPart()));
		}

		// return Optional.empty();
	}

	@Override
	public Optional<OperationOutcome> checkLogicalReference(Resource resource, ResourceReference resourceReference,
			Connection connection) throws IllegalArgumentException
	{
		return checkLogicalReference(resource, resourceReference, connection, null);
	}

	@Override
	public Optional<OperationOutcome> checkLogicalReference(Resource resource, ResourceReference reference,
			Connection connection, Integer bundleIndex) throws IllegalArgumentException
	{
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(reference, "reference");
		Objects.requireNonNull(connection, "connection");
		throwIfReferenceTypeUnexpected(reference.getType(serverBase), ReferenceType.LOGICAL);

		String targetType = reference.getReference().getType();

		Optional<ResourceDao<?>> referenceDao = daoProvider.getDao(targetType);

		if (referenceDao.isEmpty())
			return Optional.of(responseGenerator.referenceTargetTypeNotSupportedByImplementation(bundleIndex, resource,
					reference));
		else
		{
			ResourceDao<?> d = referenceDao.get();
			if (!reference.supportsType(d.getResourceType()))
				return Optional.of(
						responseGenerator.referenceTargetTypeNotSupportedByResource(bundleIndex, resource, reference));

			Identifier targetIdentifier = reference.getReference().getIdentifier();
			// Resource target =
			return search(resource, bundleIndex, connection, d, reference,
					Map.of("identifier", List.of(targetIdentifier.getSystem() + "|" + targetIdentifier.getValue())),
					true);

			// resourceReference.getReference().setIdentifier(null).setReferenceElement(
			// new IdType(target.getResourceType().name(), target.getIdElement().getIdPart()));

			// TODO add literal reference for logical reference somewhere else
			// reference.getReference().setReferenceElement(
			// new IdType(target.getResourceType().name(), target.getIdElement().getIdPart()));
		}

		// return Optional.empty();
	}

	private Optional<OperationOutcome> search(Resource resource, Integer bundleIndex, Connection connection,
			ResourceDao<?> referenceTargetDao, ResourceReference resourceReference,
			Map<String, List<String>> queryParameters, boolean logicalNotConditional)
	{
		if (Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(queryParameters::containsKey))
		{
			logger.warn(
					"Query contains parameter not applicable in this resolve reference context: '{}', parameters {} will be ignored",
					UriComponentsBuilder.newInstance()
							.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
					Arrays.toString(SearchQuery.STANDARD_PARAMETERS));

			queryParameters = queryParameters.entrySet().stream()
					.filter(e -> !Arrays.stream(SearchQuery.STANDARD_PARAMETERS).anyMatch(p -> p.equals(e.getKey())))
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		}

		SearchQuery<?> query = referenceTargetDao.createSearchQueryWithoutUserFilter(PageAndCount.exists());
		query.configureParameters(queryParameters);

		List<SearchQueryParameterError> unsupportedQueryParameters = query.getUnsupportedQueryParameters();
		if (!unsupportedQueryParameters.isEmpty())
			return Optional
					.of(responseGenerator.badReference(logicalNotConditional, bundleIndex, resource, resourceReference,
							UriComponentsBuilder.newInstance()
									.replaceQueryParams(CollectionUtils.toMultiValueMap(queryParameters)).toUriString(),
							unsupportedQueryParameters));

		PartialResult<?> result = exceptionHandler
				.handleSqlException(() -> referenceTargetDao.searchWithTransaction(connection, query));

		if (result.getTotal() <= 0)
		{
			if (logicalNotConditional)
				return Optional.of(responseGenerator.referenceTargetNotFoundLocallyByIdentifier(bundleIndex, resource,
						resourceReference));
			else
				return Optional.of(responseGenerator.referenceTargetNotFoundLocallyByCondition(bundleIndex, resource,
						resourceReference));
		}
		else if (result.getTotal() == 1)
		{
			// return result.getPartialResult().get(0);
			return Optional.empty();
		}
		else // if (result.getOverallCount() > 1)
		{
			if (logicalNotConditional)
				return Optional.of(responseGenerator.referenceTargetMultipleMatchesLocallyByIdentifier(bundleIndex,
						resource, resourceReference, result.getTotal()));
			else
				return Optional.of(responseGenerator.referenceTargetMultipleMatchesLocallyByCondition(bundleIndex,
						resource, resourceReference, result.getTotal()));
		}
	}

	@Override
	public Optional<OperationOutcome> checkCanonicalReference(Resource resource, ResourceReference reference,
			Connection connection) throws IllegalArgumentException
	{
		return checkCanonicalReference(resource, reference, connection, null);
	}

	@Override
	public Optional<OperationOutcome> checkCanonicalReference(Resource resource, ResourceReference reference,
			Connection connection, Integer bundleIndex) throws IllegalArgumentException
	{
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(reference, "reference");
		Objects.requireNonNull(connection, "connection");
		throwIfReferenceTypeUnexpected(reference.getType(serverBase), ReferenceType.CANONICAL);

		Optional<ResourceDao<?>> referenceDao = switch (reference.getLocation())
		{
			case QUESTIONNAIRE_RESPONSE_QUESTIONNAIRE -> Optional.of(daoProvider.getQuestionnaireDao());
			case TASK_INSTANTIATES_CANONICAL -> Optional.of(daoProvider.getActivityDefinitionDao());

			default -> Optional.empty();
		};

		if (referenceDao.isEmpty())
		{
			logger.debug(
					"Canonical reference check only implemented for QuestionnaireResponse.questionnaire and Task.instantiatesCanonical, not checking {}",
					reference.getLocation());
			return Optional.empty();
		}

		Optional<Resource> referencedResource = referenceDao.flatMap(dao -> search(connection, dao, reference,
				Map.of("url", List.of(reference.getCanonical().getValue())), ReferenceType.CANONICAL));

		if (referencedResource.isPresent())
			return Optional.empty();
		else
			return Optional.of(responseGenerator.referenceTargetNotFoundLocally(bundleIndex, resource, reference));
	}
}