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
package dev.dsf.fhir.help;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.utilities.validation.ValidationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.history.History;
import dev.dsf.fhir.history.HistoryEntry;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.PartialResult;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.service.ResourceReference;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.ext.RuntimeDelegate;

public class ResponseGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(ResponseGenerator.class);

	public static final CacheControl PRIVATE_NO_CACHE_NO_TRANSFORM = new CacheControl();
	static
	{
		// no-transform set by default
		PRIVATE_NO_CACHE_NO_TRANSFORM.setPrivate(true); // only user specific caching
		PRIVATE_NO_CACHE_NO_TRANSFORM.setNoCache(true); // always reevaluate with server
	}

	private final String serverBase;

	public ResponseGenerator(String serverBase)
	{
		this.serverBase = serverBase;
	}

	public OperationOutcome createOutcome(IssueSeverity severity, IssueType type, String diagnostics)
	{
		OperationOutcome outcome = new OperationOutcome();
		outcome.getIssueFirstRep().setSeverity(severity);
		outcome.getIssueFirstRep().setCode(type);
		outcome.getIssueFirstRep().setDiagnostics(diagnostics);
		return outcome;
	}

	public OperationOutcome resourceDeleted(String resourceTypeName, String id)
	{
		return createOutcome(IssueSeverity.INFORMATION, IssueType.INFORMATIONAL,
				resourceTypeName + " with id " + id + " marked as deleted");
	}

	public OperationOutcome resourceDeletedPermanently(String resourceTypeName, String id)
	{
		return createOutcome(IssueSeverity.INFORMATION, IssueType.INFORMATIONAL,
				resourceTypeName + " with id " + id + " permanently deleted");
	}

	public ResponseBuilder response(Status status, Resource resource, MediaType mediaType)
	{
		return response(status, resource, mediaType, PreferReturnType.REPRESENTATION, null);
	}

	/**
	 * @param status
	 *            not <code>null</code>
	 * @param resource
	 *            not <code>null</code>
	 * @param mediaType
	 *            may be <code>null</code>
	 * @param prefer
	 *            not <code>null</code>
	 * @param operationOutcomeCreator
	 *            not <code>null</code> if given <b>prefer</b> is {@link PreferReturnType#OPERATION_OUTCOME}
	 * @return never <code>null</code>
	 */
	public ResponseBuilder response(Status status, Resource resource, MediaType mediaType, PreferReturnType prefer,
			Supplier<OperationOutcome> operationOutcomeCreator)
	{
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(prefer, "prefer");

		if (PreferReturnType.OPERATION_OUTCOME.equals(prefer))
			Objects.requireNonNull(operationOutcomeCreator, "operationOutcomeCreator");

		ResponseBuilder b = Response.status(status);

		switch (prefer)
		{
			case REPRESENTATION:
				b = b.entity(resource);
				break;
			case OPERATION_OUTCOME:
				b = b.entity(operationOutcomeCreator.get());
				break;
			case MINIMAL:
				// do nothing, headers only
				break;
			default:
				throw new RuntimeException(PreferReturnType.class.getName() + " value " + prefer + " not supported");
		}

		if (mediaType != null)
			b = b.type(mediaType.withCharset(StandardCharsets.UTF_8.displayName()));

		if (resource.getMeta() != null && resource.getMeta().getLastUpdated() != null
				&& resource.getMeta().getVersionId() != null)
		{
			b = b.lastModified(resource.getMeta().getLastUpdated());
			b = b.tag(new EntityTag(resource.getMeta().getVersionId(), true));
		}

		b = b.cacheControl(PRIVATE_NO_CACHE_NO_TRANSFORM);

		return b;
	}

	public OperationOutcome created(URI location, Resource resource)
	{
		return created(location.toString(), resource);
	}

	public OperationOutcome created(String location, Resource resource)
	{
		String message = String.format("%s created at location %s", resource.getResourceType().name(), location);
		return createOutcome(IssueSeverity.INFORMATION, IssueType.INFORMATIONAL, message);
	}

	public OperationOutcome updated(URI location, Resource resource)
	{
		return updated(location.toString(), resource);
	}

	public OperationOutcome updated(String location, Resource resource)
	{
		String message = String.format("%s updated at location %s", resource.getResourceType().name(), location);
		return createOutcome(IssueSeverity.INFORMATION, IssueType.INFORMATIONAL, message);
	}

	/**
	 * @param result
	 *            not <code>null</code>
	 * @param errors
	 *            not <code>null</code>
	 * @param bundleUri
	 *            not <code>null</code>
	 * @param format
	 *            may be <code>null</code>
	 * @param pretty
	 *            may be <code>null</code>
	 * @param summaryMode
	 *            may be <code>null</code>
	 * @return {@link Bundle} of type {@link BundleType#SEARCHSET}
	 */
	public Bundle createSearchSet(PartialResult<? extends Resource> result, List<SearchQueryParameterError> errors,
			UriBuilder bundleUri, String format, String pretty, SummaryMode summaryMode)
	{
		Bundle bundle = new Bundle();
		bundle.setTimestamp(new Date());
		bundle.setType(BundleType.SEARCHSET);

		if (!SummaryMode.COUNT.equals(summaryMode))
		{
			result.getPartialResult().stream().map(r -> toBundleEntryComponent(r, SearchEntryMode.MATCH))
					.forEach(bundle::addEntry);
			result.getIncludes().stream().map(r -> toBundleEntryComponent(r, SearchEntryMode.INCLUDE))
					.forEach(bundle::addEntry);
		}

		if (!errors.isEmpty())
			bundle.addEntry(toBundleEntryComponent(toOperationOutcomeWarning(errors), SearchEntryMode.OUTCOME));

		bundle.setTotal(result.getTotal());

		setLinks(result.getPageAndCount(), bundleUri, format, pretty, summaryMode, bundle, result.getTotal());

		return bundle;
	}

	public BundleEntryComponent toBundleEntryComponent(Resource resource, SearchEntryMode mode)
	{
		BundleEntryComponent entry = new BundleEntryComponent();
		entry.getSearch().setMode(mode);
		entry.setResource(resource);
		entry.setFullUrlElement(new IdType(serverBase, resource.getIdElement().getResourceType(),
				resource.getIdElement().getIdPart(), null));
		return entry;
	}

	public Bundle createHistoryBundle(History history, List<SearchQueryParameterError> errors, UriBuilder bundleUri,
			String format, String pretty, SummaryMode summaryMode)
	{
		Bundle bundle = new Bundle();
		bundle.setTimestamp(new Date());
		bundle.setType(BundleType.HISTORY);

		if (!SummaryMode.COUNT.equals(summaryMode))
			history.getEntries().stream().map(this::toBundleEntryComponent).forEach(bundle::addEntry);

		if (!errors.isEmpty())
			bundle.addEntry(toBundleEntryComponent(toOperationOutcomeWarning(errors), SearchEntryMode.OUTCOME));

		bundle.setTotal(history.getTotal());

		setLinks(history.getPageAndCount(), bundleUri, format, pretty, summaryMode, bundle, history.getTotal());

		return bundle;
	}

	public BundleEntryComponent toBundleEntryComponent(HistoryEntry historyEntry)
	{
		BundleEntryComponent entry = new BundleEntryComponent();
		entry.setFullUrlElement(
				new IdType(serverBase, historyEntry.getResourceType(), historyEntry.getId().toString(), null));
		entry.getRequest().setMethod(HTTPVerb.fromCode(historyEntry.getMethod()))
				.setUrl(historyEntry.getResourceType() + (historyEntry.getResource() == null
						? "/" + historyEntry.getId().toString() + "/_history/" + historyEntry.getVersion()
						: ""));
		entry.setResource(historyEntry.getResource());
		BundleEntryResponseComponent response = entry.getResponse();

		response.setStatus(toStatus(historyEntry.getMethod()));
		response.setLocation(
				toLocation(historyEntry.getResourceType(), historyEntry.getId().toString(), historyEntry.getVersion()));
		response.setEtag(RuntimeDelegate.getInstance().createHeaderDelegate(EntityTag.class)
				.toString(new EntityTag(historyEntry.getVersion(), true)));
		response.setLastModified(Date.from(historyEntry.getLastUpdated().atZone(ZoneId.systemDefault()).toInstant()));

		return entry;
	}

	private String toStatus(String method)
	{
		return switch (method)
		{
			case "POST" -> "201 Created";
			case "PUT" -> "200 OK";
			case "DELETE" -> "200 OK";
			default -> throw new RuntimeException("Method " + method + " not supported");
		};
	}

	private String toLocation(String resourceType, String id, String version)
	{
		return new IdType(serverBase, resourceType, id, version).getValue();
	}

	private void setLinks(PageAndCount pageAndCount, UriBuilder bundleUri, String format, String pretty,
			SummaryMode summaryMode, Bundle bundle, int total)
	{
		if (format != null)
			bundleUri = bundleUri.replaceQueryParam("_format", format);
		if (pretty != null)
			bundleUri = bundleUri.replaceQueryParam("_pretty", pretty);
		if (summaryMode != null)
			bundleUri = bundleUri.replaceQueryParam("_summary", summaryMode.toString());

		boolean countOnly = pageAndCount.isCountOnly(total);
		if (!countOnly)
		{
			bundleUri = bundleUri.replaceQueryParam("_count", pageAndCount.getCount());
			bundleUri = bundleUri.replaceQueryParam("_page", pageAndCount.getPage());
		}
		else
			bundleUri = bundleUri.replaceQueryParam("_count", "0");

		bundle.addLink().setRelation("self").setUrlElement(new UriType(bundleUri.build()));

		if (!countOnly && pageAndCount.getCount() > 0)
		{
			bundleUri = bundleUri.replaceQueryParam("_page", 1);
			bundleUri = bundleUri.replaceQueryParam("_count", pageAndCount.getCount());
			bundle.addLink().setRelation("first").setUrlElement(new UriType(bundleUri.build()));

			if (pageAndCount.getPage() > 1)
			{
				bundleUri = bundleUri.replaceQueryParam("_page", pageAndCount.getPage() - 1);
				bundleUri = bundleUri.replaceQueryParam("_count", pageAndCount.getCount());
				bundle.addLink().setRelation("previous").setUrlElement(new UriType(bundleUri.build()));
			}
			if (!pageAndCount.isLastPage(total))
			{
				bundleUri = bundleUri.replaceQueryParam("_page", pageAndCount.getPage() + 1);
				bundleUri = bundleUri.replaceQueryParam("_count", pageAndCount.getCount());
				bundle.addLink().setRelation("next").setUrlElement(new UriType(bundleUri.build()));
			}

			bundleUri = bundleUri.replaceQueryParam("_page", pageAndCount.getLastPage(total));
			bundleUri = bundleUri.replaceQueryParam("_count", pageAndCount.getCount());
			bundle.addLink().setRelation("last").setUrlElement(new UriType(bundleUri.build()));
		}
	}

	public OperationOutcome toOperationOutcomeWarning(List<SearchQueryParameterError> errors)
	{
		return toOperationOutcome(errors, IssueSeverity.WARNING);
	}

	public OperationOutcome toOperationOutcomeError(List<SearchQueryParameterError> errors)
	{
		return toOperationOutcome(errors, IssueSeverity.ERROR);
	}

	private OperationOutcome toOperationOutcome(List<SearchQueryParameterError> errors, IssueSeverity severity)
	{
		String diagnostics = errors.stream().map(SearchQueryParameterError::toString).collect(Collectors.joining("; "));
		return createOutcome(severity, IssueType.PROCESSING, diagnostics);
	}

	public Response pathVsElementId(String resourceTypeName, String id, IdType resourceId)
	{
		logger.warn("Path id not equal to {}.id", resourceTypeName);

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Path id not equal to " + resourceTypeName + " id (" + id + " vs. " + resourceId.getIdPart() + ")");
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response invalidBaseUrl(String resourceTypeName, IdType resourceId)
	{
		logger.warn("{} id.baseUrl must be null or equal to {}, value {} unexpected", resourceTypeName, serverBase,
				resourceId.getBaseUrl());

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				resourceTypeName + " id.baseUrl must be null or equal to " + serverBase + ", value "
						+ resourceId.getBaseUrl() + " unexpected");
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response badRequest(String queryParameters, List<SearchQueryParameterError> unsupportedQueryParameters)
	{
		String unsupportedQueryParametersString = unsupportedQueryParameters.stream()
				.map(SearchQueryParameterError::toString).collect(Collectors.joining("; "));
		logger.warn("Bad request, {} unsupported query parameter{}", unsupportedQueryParameters.size(),
				unsupportedQueryParameters.size() != 1 ? "s" : "");

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad request '" + queryParameters + "', unsupported query parameter"
						+ (unsupportedQueryParameters.size() != 1 ? "s" : "") + " " + unsupportedQueryParametersString);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response badRequestIdsNotMatching(IdType dbResourceId, IdType resourceId)
	{
		logger.warn("Bad request Id {} does not match db Id {}", resourceId.getValue(), dbResourceId.getValue());

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad request Id " + resourceId.getValue() + " does not match db Id " + dbResourceId.getValue());
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response updateAsCreateNotAllowed(String resourceTypeName)
	{
		logger.warn("Update as create of resource with type {} not allowed", resourceTypeName);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.FORBIDDEN,
				"Update as create not allowed");
		return Response.status(Status.METHOD_NOT_ALLOWED).entity(outcome).build();
	}

	public Response multipleExists(String resourceTypeName, String ifNoneExistsHeaderValue)
	{
		logger.warn("Multiple {} resources with If-None-Exist criteria exist", resourceTypeName);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.MULTIPLEMATCHES,
				"Multiple " + resourceTypeName + " resources with criteria '" + ifNoneExistsHeaderValue + "' exist");
		return Response.status(Status.PRECONDITION_FAILED).entity(outcome).build();
	}

	public Response duplicateResourceExists()
	{
		logger.warn("Duplicate resources exists");

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.DUPLICATE, "Duplicate resources exist");
		return Response.status(Status.FORBIDDEN).entity(outcome).build();
	}

	public Response duplicateResourceExists(String resourceTypeName)
	{
		logger.warn("Duplicate {} resources exists", resourceTypeName);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.DUPLICATE,
				"Duplicate " + resourceTypeName + " resources exist");
		return Response.status(Status.FORBIDDEN).entity(outcome).build();
	}

	public Response badIfNoneExistHeaderValue(String logMessageReason, String ifNoneExistsHeaderValue)
	{
		logger.warn("Bad If-None-Exist header value: {}", logMessageReason);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad If-None-Exist header value '" + ifNoneExistsHeaderValue + "'");
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response badIfNoneExistHeaderValue(String ifNoneExistsHeaderValue,
			List<SearchQueryParameterError> unsupportedQueryParameters)
	{
		String unsupportedQueryParametersString = unsupportedQueryParameters.stream()
				.map(SearchQueryParameterError::toString).collect(Collectors.joining("; "));
		logger.warn("Bad If-None-Exist header value, {} unsupported query parameter{}",
				unsupportedQueryParameters.size(), unsupportedQueryParameters.size() != 1 ? "s" : "");

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad If-None-Exist header value '" + ifNoneExistsHeaderValue + "', unsupported query parameter"
						+ (unsupportedQueryParameters.size() != 1 ? "s" : "") + " " + unsupportedQueryParametersString);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response oneExists(Resource resource, String ifNoneExistsHeaderValue)
	{
		logger.info("{} with criteria from 'If-None-Exist' header exists", resource.getResourceType().name());

		OperationOutcome outcome = createOutcome(IssueSeverity.INFORMATION, IssueType.DUPLICATE,
				"Resource with criteria '" + ifNoneExistsHeaderValue + "' exists");

		UriBuilder uri = UriBuilder.fromPath(serverBase);
		URI location = uri.path("/{resourceType}/{id}/" + Constants.PARAM_HISTORY + "/{vid}").build(
				resource.getResourceType().name(), resource.getIdElement().getIdPart(),
				resource.getIdElement().getVersionIdPart());

		return Response.status(Status.OK).entity(outcome).location(location)
				.lastModified(resource.getMeta().getLastUpdated())
				.tag(new EntityTag(resource.getMeta().getVersionId(), true)).cacheControl(PRIVATE_NO_CACHE_NO_TRANSFORM)
				.header(HttpHeaders.VARY, HttpHeaders.COOKIE).build();
	}

	public OperationOutcome unknownReference(Resource resource, ResourceReference resourceReference)
	{
		return unknownReference(resource, resourceReference, null);
	}

	public OperationOutcome unknownReference(Resource resource, ResourceReference resourceReference,
			Integer bundleIndex)
	{
		if (bundleIndex == null)
			logger.warn("Unknown reference at {} in resource of type {} with id {}", resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId());
		else
			logger.warn("Unknown reference at {} in resource of type {} with id {} at bundle index {}",
					resourceReference.getLocation(), resource.getResourceType().name(), resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Unknown reference at " + resourceReference.getLocation() + " in resource of type "
						+ resource.getResourceType().name() + " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex));
	}

	public OperationOutcome referenceTargetTypeNotSupportedByImplementation(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference)
	{
		if (bundleIndex == null)
			logger.warn(
					"Reference target type of reference at {} in resource of type {} with id {} not supported by this implementation",
					resourceReference.getLocation(), resource.getResourceType().name(), resource.getId());
		else
			logger.warn(
					"Reference target type of reference at {} in resource of type {} with id {} at bundle index {} not supported by this implementation",
					resourceReference.getLocation(), resource.getResourceType().name(), resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Reference target type of reference at " + resourceReference.getLocation() + " in resource of type "
						+ resource.getResourceType().name() + " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex)
						+ " not supported by this implementation");
	}

	public OperationOutcome referenceTargetTypeNotSupportedByResource(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference)
	{
		if (bundleIndex == null)
			logger.warn("Reference target type of reference at {} in resource of type {} with id {} not supported",
					resourceReference.getLocation(), resource.getResourceType().name(), resource.getId());
		else
			logger.warn(
					"Reference target type of reference at {} in resource of type {} with id {} at bundle index {} not supported",
					resourceReference.getLocation(), resource.getResourceType().name(), resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Reference target type of reference at " + resourceReference.getLocation() + " in resource of type "
						+ resource.getResourceType().name() + " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not supported");
	}

	public OperationOutcome referenceTargetNotFoundLocally(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference)
	{
		if (bundleIndex == null)
			logger.warn("Reference target {} of reference at {} in resource of type {} with id {} not found",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId());
		else
			logger.warn(
					"Reference target {} of reference at {} in resource of type {} with id {} at bundle index {} not found",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Reference target " + resourceReference.getValue() + " of reference at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not found");
	}

	public OperationOutcome referenceTargetNotFoundRemote(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference, String serverBase)
	{
		if (bundleIndex == null)
			logger.warn(
					"Reference target {} of reference at {} in resource of type {} with id {} not found on server {}",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId(), serverBase);
		else
			logger.warn(
					"Reference target {} of reference at {} in resource of type {} with id {} at bundle index {} not found on server {}",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId(), bundleIndex, serverBase);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Reference target " + resourceReference.getValue() + " of reference at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not found on server "
						+ serverBase);
	}

	public OperationOutcome referenceTargetCouldNotBeResolvedOnRemote(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference, String serverBase)
	{
		if (bundleIndex == null)
			logger.warn(
					"Reference target {} of reference at {} in resource of type {} with id {} could not be resolved on server {} (reason hidden)",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId(), serverBase);
		else
			logger.warn(
					"Reference target {} of reference at {} in resource of type {} with id {} at bundle index {} could not be resolved on server {} (reason hidden)",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId(), bundleIndex, serverBase);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Reference target " + resourceReference.getValue() + " of reference at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex)
						+ " could not be resolved on server " + serverBase + " (reason hidden)");
	}

	public OperationOutcome noEndpointFoundForLiteralExternalReference(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference)
	{
		if (bundleIndex == null)
			logger.warn(
					"No Endpoint found for reference target {} of reference at {} in resource of type {} with id {}",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId());
		else
			logger.warn(
					"No Endpoint found for reference target {} of reference at {} in resource of type {} with id {} at bundle index {} not found",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"No Endpoint found for reference target " + resourceReference.getValue() + " of reference at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not found");
	}

	public OperationOutcome badReference(boolean logicalNotConditional, Integer bundleIndex, Resource resource,
			ResourceReference resourceReference, String queryParameters,
			List<SearchQueryParameterError> unsupportedQueryParameters)
	{
		String unsupportedQueryParametersString = unsupportedQueryParameters.stream()
				.map(SearchQueryParameterError::toString).collect(Collectors.joining("; "));

		if (bundleIndex == null)
			logger.warn(
					"{} reference {} at {} in resource of type {} with id {} contains unsupported queryparameter{} {}",
					logicalNotConditional ? "Logical" : "Conditional", queryParameters, resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId(),
					unsupportedQueryParameters.size() != 1 ? "s" : "", unsupportedQueryParametersString);
		else
			logger.warn(
					"{} reference {} at {} in resource of type {} with id {} at bundle index {} contains unsupported queryparameter{} {}",
					logicalNotConditional ? "Logical" : "Conditional", queryParameters, resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId(), bundleIndex,
					unsupportedQueryParameters.size() != 1 ? "s" : "", unsupportedQueryParametersString);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				(logicalNotConditional ? "Logical" : "Conditional") + " reference " + queryParameters + " at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex)
						+ " contains unsupported queryparameter" + (unsupportedQueryParameters.size() != 1 ? "s" : "")
						+ " " + unsupportedQueryParametersString);
	}

	public OperationOutcome referenceTargetNotFoundLocallyByIdentifier(Resource resource,
			ResourceReference resourceReference)
	{
		return referenceTargetNotFoundLocallyByIdentifier(null, resource, resourceReference);
	}

	public OperationOutcome referenceTargetNotFoundLocallyByIdentifier(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference)
	{
		if (bundleIndex == null)
			logger.warn(
					"Reference target by identifier '{}|{}' of reference at {} in resource of type {} with id {} not found",
					resourceReference.getReference().getIdentifier().getSystem(),
					resourceReference.getReference().getIdentifier().getValue(), resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId());
		else
			logger.warn(
					"Reference target by identifier '{}|{}' of reference at {} in resource of type {} with id {} at bundle index {} not found",
					resourceReference.getReference().getIdentifier().getSystem(),
					resourceReference.getReference().getIdentifier().getValue(), resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Reference target by identifier '" + resourceReference.getReference().getIdentifier().getSystem() + "|"
						+ resourceReference.getReference().getIdentifier().getValue() + "' of reference at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not found");
	}

	public OperationOutcome referenceTargetMultipleMatchesLocallyByIdentifier(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference, int overallCount)
	{
		if (bundleIndex == null)
			logger.warn(
					"Found {} matches for reference target by identifier '{}|{}' of reference at {} in resource of type {} with id {}",
					overallCount, resourceReference.getReference().getIdentifier().getSystem(),
					resourceReference.getReference().getIdentifier().getValue(), resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId());
		else
			logger.warn(
					"Found {} matches for reference target by identifier '{}|{}' of reference at {} in resource of type {} with id {} at bundle index {}",
					overallCount, resourceReference.getReference().getIdentifier().getSystem(),
					resourceReference.getReference().getIdentifier().getValue(), resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Found " + overallCount + " matches for reference target by identifier '"
						+ resourceReference.getReference().getIdentifier().getSystem() + "|"
						+ resourceReference.getReference().getIdentifier().getValue() + "' of reference at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not found");
	}

	public OperationOutcome referenceTargetNotFoundLocallyByCondition(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference)
	{
		if (bundleIndex == null)
			logger.warn(
					"Reference target by condition '{}' of reference at {} in resource of type {} with id {} not found",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId());
		else
			logger.warn(
					"Reference target by condition '{}' of reference at {} in resource of type {} with id {} at bundle index {} not found",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Reference target by condition '" + resourceReference.getValue() + "' of reference at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not found");
	}

	public OperationOutcome referenceTargetMultipleMatchesLocallyByCondition(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference, int overallCount)
	{
		if (bundleIndex == null)
			logger.warn(
					"Found {} matches for reference target by condition '{}' of reference at {} in resource of type {} with id {}",
					overallCount, resourceReference.getValue(), resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId());
		else
			logger.warn(
					"Found {} matches for reference target by condition '{}' of reference at {} in resource of type {} with id {} at bundle index {}",
					overallCount, resourceReference.getValue(), resourceReference.getLocation(),
					resource.getResourceType().name(), resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Found " + overallCount + " matches for reference target by condition '" + resourceReference.getValue()
						+ "' of reference at " + resourceReference.getLocation() + " in resource of type "
						+ resource.getResourceType().name() + " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not found");
	}

	public OperationOutcome referenceTargetBadCondition(Integer bundleIndex, Resource resource,
			ResourceReference resourceReference)
	{
		if (bundleIndex == null)
			logger.warn("Bad conditional reference target '{}' of reference at {} in resource of type {} with id {}",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId());
		else
			logger.warn(
					"Bad conditional reference target '{}' of reference at {} in resource of type {} with id {} at bundle index {}",
					resourceReference.getValue(), resourceReference.getLocation(), resource.getResourceType().name(),
					resource.getId(), bundleIndex);

		return createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad conditional reference target '" + resourceReference.getValue() + "' of reference at "
						+ resourceReference.getLocation() + " in resource of type " + resource.getResourceType().name()
						+ " with id " + resource.getId()
						+ (bundleIndex == null ? "" : " at bundle index " + bundleIndex) + " not found");
	}

	public Response badDeleteRequestUrl(int bundleIndex, String url)
	{
		logger.warn("Bad delete request url {} at bundle index {}", url, bundleIndex);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad delete request url " + url + " at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response badCreateRequestUrl(int bundleIndex, String url)
	{
		logger.warn("Bad crate request url {} at bundle index {}", url, bundleIndex);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad crete request url " + url + " at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response badUpdateRequestUrl(int bundleIndex, String url)
	{
		logger.warn("Bad update request url {} at bundle index {}", url, bundleIndex);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad update request url " + url + " at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response badReadRequestUrl(int bundleIndex, String url)
	{
		logger.warn("Bad read request url {} at bundle index {}", url, bundleIndex);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad read request url " + url + " at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response resourceTypeNotSupportedByImplementation(int bundleIndex, String resourceTypeName)
	{
		logger.warn("Resource type {} at bundle index {} not supported by this implementation", resourceTypeName,
				bundleIndex);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING, "Resource type "
				+ resourceTypeName + " at bundle index " + bundleIndex + " not supported by this implementation");
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response badConditionalDeleteRequest(int bundleIndex, String queryParameters,
			List<SearchQueryParameterError> unsupportedQueryParameters)
	{
		String unsupportedQueryParametersString = unsupportedQueryParameters.stream()
				.map(SearchQueryParameterError::toString).collect(Collectors.joining("; "));
		logger.warn("Bad conditional delete request '{}', unsupported query parameter{} {} at bundle index {}",
				queryParameters, unsupportedQueryParameters.size() != 1 ? "s" : "", unsupportedQueryParametersString,
				bundleIndex);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad conditional delete request '" + queryParameters + "', unsupported query parameter"
						+ (unsupportedQueryParameters.size() != 1 ? "s" : "") + " " + unsupportedQueryParametersString
						+ " at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response badConditionalDeleteRequestMultipleMatches(int bundleIndex, String resourceTypeName,
			String queryParameters)
	{
		logger.warn("Multiple {} resources with criteria '{}' exist for delete request at bundle index {}",
				resourceTypeName, queryParameters, bundleIndex);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.MULTIPLEMATCHES,
				"Multiple " + resourceTypeName + " resources with criteria '" + queryParameters
						+ "' exist for delete request at bundle index " + bundleIndex);
		return Response.status(Status.PRECONDITION_FAILED).entity(outcome).build();
	}

	public Response badBundleRequest(String message)
	{
		logger.warn("Bad bundle request - {}", message);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad bundle request - " + message);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	public Response pathVsElementIdInBundle(int bundleIndex, String resourceTypeName, String id, IdType resourceId)
	{
		logger.warn("Path id not equal to {} id ({} vs. {}) at bundle index {}", resourceTypeName, id,
				resourceId.getIdPart(), bundleIndex);

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Path id not equal to " + resourceTypeName + " id (" + id + " vs. " + resourceId.getIdPart()
						+ ") at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response invalidBaseUrlInBundle(int bundleIndex, String resourceTypeName, IdType resourceId)
	{
		logger.warn("{} id.baseUrl must be null or equal to {}, value {} unexpected at bundle index {}",
				resourceTypeName, serverBase, resourceId.getBaseUrl(), bundleIndex);

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				resourceTypeName + " id.baseUrl must be null or equal to " + serverBase + ", value "
						+ resourceId.getBaseUrl() + " unexpected at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response nonMatchingResourceTypeAndRequestUrlInBundle(int bundleIndex, String resourceTypeName, String url)
	{
		logger.warn("Non matching resource type {} and request url {} at bundle index {}", resourceTypeName, url,
				bundleIndex);

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING, "Non matching resource type "
				+ resourceTypeName + " and request url " + url + " at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response unsupportedConditionalUpdateQuery(int bundleIndex, String query,
			List<SearchQueryParameterError> unsupportedQueryParameters)
	{
		String unsupportedQueryParametersString = unsupportedQueryParameters.stream()
				.map(SearchQueryParameterError::toString).collect(Collectors.joining("; "));

		logger.warn("Bad conditional update request '{}', unsupported query parameter{} {} at bundle index {}", query,
				unsupportedQueryParameters.size() != 1 ? "s" : "", unsupportedQueryParametersString, bundleIndex);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad conditional update request '" + query + "', unsupported query parameter"
						+ (unsupportedQueryParameters.size() != 1 ? "s" : "") + " " + unsupportedQueryParametersString
						+ " at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(outcome).build();

	}

	public Response bundleEntryResouceMissingId(int bundleIndex, String resourceTypeName)
	{
		logger.warn("Bundle entry of type {} at bundle index {} is missing id value", resourceTypeName, bundleIndex);

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING, "Bundle entry of type "
				+ resourceTypeName + " at bundle index " + bundleIndex + " is missing id value");
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response badBundleEntryFullUrl(int bundleIndex, String fullUrl)
	{
		logger.warn("Bad entry fullUrl '{}' at bundle index {}", fullUrl, bundleIndex);

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bad entry fullUrl '" + fullUrl + "' at bundle index " + bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response bundleEntryBadResourceId(int bundleIndex, String resourceTypeName, String urlUuidPrefix)
	{
		logger.warn("Bundle entry of type {} at bundle index {} id value not starting with {}", resourceTypeName,
				bundleIndex, urlUuidPrefix);

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Bundle entry of type " + resourceTypeName + " at bundle index " + bundleIndex
						+ " id value not starting with " + urlUuidPrefix);
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response badBundleEntryFullUrlVsResourceId(int bundleIndex, String fullUrl, String idValue)
	{
		logger.warn("Resource id not equal to entry fullUrl ({} vs. {}) at bundle index {}", idValue, fullUrl,
				bundleIndex);

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Resource id not equal to entry fullUrl (" + idValue + " vs. " + fullUrl + ") at bundle index "
						+ bundleIndex);
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response forbiddenNotAllowed(String operation, Identity identity)
	{
		logger.warn("Operation {} forbidden for identity '{}'", operation, identity.getName());

		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.FORBIDDEN,
				"Operation " + operation + " forbidden");
		return Response.status(Status.FORBIDDEN).entity(out).build();
	}

	public Response notFound(String id, String resourceTypeName)
	{
		logger.warn("{} with id {} not found", resourceTypeName, id);

		OperationOutcome outcome = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				resourceTypeName + " with id " + id + " not found");
		return Response.status(Status.NOT_FOUND).entity(outcome).build();
	}

	public Response forbiddenNotValid(String operation, Identity identity, String resourceType,
			ValidationResult validationResult)
	{
		OperationOutcome outcome = new OperationOutcome();
		validationResult.populateOperationOutcome(outcome);

		logger.warn("Operation {} forbidden, {} resource not valid for user '{}'", operation, resourceType,
				identity.getName());

		return Response.status(Status.FORBIDDEN).entity(outcome).build();
	}

	public Response unableToGenerateSnapshot(StructureDefinition resource, Integer bundleIndex,
			List<ValidationMessage> messages)
	{
		String messagesLogString = messages == null ? ""
				: messages.stream().map(ValidationMessage::getDisplay).collect(Collectors.joining(", ", ": [", "]"));

		if (bundleIndex == null)
			logger.warn(
					"Unable to generate StructureDefinition snapshot for profile with url {}, version {} and id {}{}",
					resource.getUrl(), resource.getVersion(), resource.getId(), messagesLogString);
		else
			logger.warn(
					"Unable to generate StructureDefinition snapshot for profile with url {}, version {} and id {} at bundle index {}{}",
					resource.getUrl(), resource.getVersion(), resource.getId(), bundleIndex, messagesLogString);

		OperationOutcome outcome = new OperationOutcome();

		if (messages != null)
			messages.forEach(m -> outcome.addIssue().setSeverity(convert(m.getLevel())).setCode(convert(m.getType()))
					.setDiagnostics(m.summary()));

		return Response.status(Status.BAD_REQUEST).entity(outcome).build();
	}

	private IssueSeverity convert(ValidationMessage.IssueSeverity severity)
	{
		return switch (severity)
		{
			case FATAL -> IssueSeverity.FATAL;
			case ERROR -> IssueSeverity.ERROR;
			case WARNING -> IssueSeverity.WARNING;
			case INFORMATION -> IssueSeverity.INFORMATION;
			case NULL -> IssueSeverity.NULL;
		};
	}

	private IssueType convert(ValidationMessage.IssueType type)
	{
		return switch (type)
		{
			case INVALID -> IssueType.INVALID;
			case STRUCTURE -> IssueType.STRUCTURE;
			case REQUIRED -> IssueType.REQUIRED;
			case VALUE -> IssueType.VALUE;
			case INVARIANT -> IssueType.INVARIANT;
			case SECURITY -> IssueType.SECURITY;
			case LOGIN -> IssueType.LOGIN;
			case UNKNOWN -> IssueType.UNKNOWN;
			case EXPIRED -> IssueType.EXPIRED;
			case FORBIDDEN -> IssueType.FORBIDDEN;
			case SUPPRESSED -> IssueType.SUPPRESSED;
			case PROCESSING -> IssueType.PROCESSING;
			case NOTSUPPORTED -> IssueType.NOTSUPPORTED;
			case DUPLICATE -> IssueType.DUPLICATE;
			case MULTIPLEMATCHES -> IssueType.MULTIPLEMATCHES;
			case NOTFOUND -> IssueType.NOTFOUND;
			case DELETED -> IssueType.DELETED;
			case TOOLONG -> IssueType.TOOLONG;
			case CODEINVALID -> IssueType.CODEINVALID;
			case EXTENSION -> IssueType.EXTENSION;
			case TOOCOSTLY -> IssueType.TOOCOSTLY;
			case BUSINESSRULE -> IssueType.BUSINESSRULE;
			case CONFLICT -> IssueType.CONFLICT;
			case TRANSIENT -> IssueType.TRANSIENT;
			case LOCKERROR -> IssueType.LOCKERROR;
			case NOSTORE -> IssueType.NOSTORE;
			case EXCEPTION -> IssueType.EXCEPTION;
			case TIMEOUT -> IssueType.TIMEOUT;
			case INCOMPLETE -> IssueType.INCOMPLETE;
			case THROTTLED -> IssueType.THROTTLED;
			case INFORMATIONAL -> IssueType.INFORMATIONAL;
			case NULL -> IssueType.NULL;
		};
	}
}
