package org.highmed.fhir.help;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.highmed.fhir.search.PartialResult;
import org.highmed.fhir.search.SearchQueryParameterError;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.UriType;

public class ResponseGenerator
{
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

	public ResponseBuilder response(Status status, Resource resource, MediaType mediaType)
	{
		Objects.requireNonNull(status, "status");
		Objects.requireNonNull(resource, "resource");

		ResponseBuilder b = Response.status(status).entity(resource);

		if (mediaType != null)
			b = b.type(mediaType);

		if (resource.getMeta() != null && resource.getMeta().getLastUpdated() != null
				&& resource.getMeta().getVersionId() != null)
		{
			b = b.lastModified(resource.getMeta().getLastUpdated());
			b = b.tag(new EntityTag(resource.getMeta().getVersionId(), true));
		}

		return b;
	}

	public IdType toFullId(IdType id)
	{
		id.setIdBase(serverBase);
		return id;
	}

	public String toFullId(String id)
	{
		if (id == null)
			return null;

		return toFullId(new IdType(id)).asStringValue();
	}

	public BundleEntryComponent toBundleEntryComponent(DomainResource resource, SearchEntryMode mode)
	{
		BundleEntryComponent entry = new BundleEntryComponent();
		entry.getSearch().setMode(mode);
		entry.setResource(resource);
		entry.setFullUrl(toFullId(resource.getId()));
		return entry;
	}

	public Bundle createSearchSet(PartialResult<? extends DomainResource> result,
			List<SearchQueryParameterError> errors, UriBuilder bundleUri, String format, String pretty)
	{
		Bundle bundle = new Bundle();
		bundle.setId(UUID.randomUUID().toString());
		bundle.getMeta().setLastUpdated(new Date());
		bundle.setType(BundleType.SEARCHSET);
		result.getPartialResult().stream().map(r -> toBundleEntryComponent(r, SearchEntryMode.MATCH))
				.forEach(bundle::addEntry);
		result.getIncludes().stream().map(r -> toBundleEntryComponent(r, SearchEntryMode.INCLUDE))
				.forEach(bundle::addEntry);
		if (!errors.isEmpty())
			bundle.addEntry(toBundleEntryComponent(toOperationOutcome(errors), SearchEntryMode.OUTCOME));

		bundle.setTotal(result.getOverallCount());

		if (format != null)
			bundleUri = bundleUri.replaceQueryParam("_format", format);
		if (pretty != null)
			bundleUri = bundleUri.replaceQueryParam("_pretty", pretty);

		if (result.getPageAndCount().getCount() > 0)
		{
			bundleUri = bundleUri.replaceQueryParam("_count", result.getPageAndCount().getCount());
			bundleUri = bundleUri.replaceQueryParam("_page",
					result.getPartialResult().isEmpty() ? 1 : result.getPageAndCount().getPage());
		}
		else
			bundleUri = bundleUri.replaceQueryParam("_count", "0");

		bundle.addLink().setRelation("self").setUrlElement(new UriType(bundleUri.build()));

		if (result.getPageAndCount().getCount() > 0 && !result.getPartialResult().isEmpty())
		{
			bundleUri = bundleUri.replaceQueryParam("_page", 1);
			bundleUri = bundleUri.replaceQueryParam("_count", result.getPageAndCount().getCount());
			bundle.addLink().setRelation("first").setUrlElement(new UriType(bundleUri.build()));

			if (result.getPageAndCount().getPage() > 1)
			{
				bundleUri = bundleUri.replaceQueryParam("_page", result.getPageAndCount().getPage() - 1);
				bundleUri = bundleUri.replaceQueryParam("_count", result.getPageAndCount().getCount());
				bundle.addLink().setRelation("previous").setUrlElement(new UriType(bundleUri.build()));
			}
			if (!result.isLastPage())
			{
				bundleUri = bundleUri.replaceQueryParam("_page", result.getPageAndCount().getPage() + 1);
				bundleUri = bundleUri.replaceQueryParam("_count", result.getPageAndCount().getCount());
				bundle.addLink().setRelation("next").setUrlElement(new UriType(bundleUri.build()));
			}

			bundleUri = bundleUri.replaceQueryParam("_page", result.getLastPage());
			bundleUri = bundleUri.replaceQueryParam("_count", result.getPageAndCount().getCount());
			bundle.addLink().setRelation("last").setUrlElement(new UriType(bundleUri.build()));
		}

		return bundle;
	}

	public OperationOutcome toOperationOutcome(List<SearchQueryParameterError> errors)
	{
		String diagnostics = errors.stream().map(SearchQueryParameterError::toString).collect(Collectors.joining("; "));
		return createOutcome(IssueSeverity.WARNING, IssueType.PROCESSING, diagnostics);
	}

	public Response createPathVsElementIdResponse(String resourceTypeName, String id, IdType resourceId)
	{
		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				"Path id not equal to " + resourceTypeName + " id (" + id + " vs. " + resourceId.getIdPart() + ").");
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}

	public Response createInvalidBaseUrlResponse(String resourceTypeName, IdType resourceId)
	{
		OperationOutcome out = createOutcome(IssueSeverity.ERROR, IssueType.PROCESSING,
				resourceTypeName + " id.baseUrl must be null or equal to " + serverBase + ", value "
						+ resourceId.getBaseUrl() + " unexpected.");
		return Response.status(Status.BAD_REQUEST).entity(out).build();
	}
}
