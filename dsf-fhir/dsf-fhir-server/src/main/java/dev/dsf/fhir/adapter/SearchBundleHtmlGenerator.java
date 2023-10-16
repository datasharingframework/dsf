package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.glassfish.jersey.uri.UriComponent;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskStatus;
import org.springframework.web.util.HtmlUtils;

import jakarta.ws.rs.core.MultivaluedMap;

public class SearchBundleHtmlGenerator extends InputHtmlGenerator implements HtmlGenerator<Bundle>
{
	private static final SimpleDateFormat DATE_TIME_DISPLAY_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	private static final String INSTANTIATES_CANONICAL_PATTERN_STRING = "(?<processUrl>http[s]{0,1}://(?<domain>(?:(?:[a-zA-Z0-9]{1,63}|[a-zA-Z0-9][a-zA-Z0-9-]{0,61}[a-zA-Z0-9])\\.)+(?:[a-zA-Z0-9]{1,63}))"
			+ "/bpe/Process/(?<processName>[a-zA-Z0-9-]+))\\|(?<processVersion>\\d+\\.\\d+)$";
	private static final Pattern INSTANTIATES_CANONICAL_PATTERN = Pattern
			.compile(INSTANTIATES_CANONICAL_PATTERN_STRING);

	private static final String CODE_SYSTEM_BPMN_MESSAGE = "http://dsf.dev/fhir/CodeSystem/bpmn-message";
	private static final String CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME = "message-name";
	private static final String CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY = "business-key";
	private static final String CODE_SYSTEM_ORGANIZATION_ROLE = "http://dsf.dev/fhir/CodeSystem/organization-role";
	private static final String NAMING_SYSTEM_ENDPOINT_IDENTIFIER = "http://dsf.dev/sid/endpoint-identifier";
	private static final String NAMING_SYSTEM_ORGANIZATION_IDENTIFIER = "http://dsf.dev/sid/organization-identifier";
	private static final String NAMING_SYSTEM_TASK_IDENTIFIER = "http://dsf.dev/sid/task-identifier";

	private final String taskRessourcePath;
	private final String questionnaireResponseRessourcePath;
	private final String organizationResourcePath;
	private final String organizationAffiliationResourcePath;
	private final String endpointResourcePath;

	private final int defaultPageCount;

	public SearchBundleHtmlGenerator(String serverBaseUrl, int defaultPageCount)
	{
		String serverBaseUrlPath = getServerBaseUrlPath(serverBaseUrl);
		taskRessourcePath = serverBaseUrlPath + "/" + ResourceType.Task.name();
		questionnaireResponseRessourcePath = serverBaseUrlPath + "/" + ResourceType.QuestionnaireResponse.name();
		organizationResourcePath = serverBaseUrlPath + "/" + ResourceType.Organization.name();
		organizationAffiliationResourcePath = serverBaseUrlPath + "/" + ResourceType.OrganizationAffiliation.name();
		endpointResourcePath = serverBaseUrlPath + "/" + ResourceType.Endpoint.name();

		this.defaultPageCount = defaultPageCount;
	}

	private String getServerBaseUrlPath(String serverBaseUrl)
	{
		try
		{
			return new URL(serverBaseUrl).getPath();
		}
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public Class<Bundle> getResourceType()
	{
		return Bundle.class;
	}

	@Override
	public boolean isResourceSupported(URI resourceUri, Resource resource)
	{
		return resource != null && resource instanceof Bundle
				&& (taskRessourcePath.equals(resourceUri.getPath())
						|| questionnaireResponseRessourcePath.equals(resourceUri.getPath())
						|| organizationResourcePath.equals(resourceUri.getPath())
						|| organizationAffiliationResourcePath.equals(resourceUri.getPath())
						|| endpointResourcePath.equals(resourceUri.getPath()));
	}

	@Override
	public void writeHtml(URI resourceUri, Bundle resource, OutputStreamWriter out) throws IOException
	{
		out.write("<div class=\"bundle\">");
		out.write("<div id=\"header\">");
		out.write("<table><tr><td>");

		Optional<String> first = resource.getLink().stream().filter(l -> "first".equals(l.getRelation())).findFirst()
				.map(BundleLinkComponent::getUrl);

		if (first.isPresent())
			out.write("<a href=\"" + first.get() + "\" title=\"First Page\">");
		out.write("<svg class=\"icon\" " + (first.isEmpty() ? " disabled" : "")
				+ " height=\"24\" viewBox=\"0 -960 960 960\" width=\"24\"><path d=\"M240-240v-480h60v480h-60Zm447-3L453-477l234-234 43 43-191 191 191 191-43 43Z\"/></svg>");
		if (first.isPresent())
			out.write("</a>");

		Optional<String> previous = resource.getLink().stream().filter(l -> "previous".equals(l.getRelation()))
				.findFirst().map(BundleLinkComponent::getUrl);
		if (previous.isPresent())
			out.write("<a href=\"" + previous.get() + "\" title=\"Previous Page\">");
		out.write("<svg class=\"icon\"" + (previous.isEmpty() ? " disabled" : "")
				+ " height=\"24\" viewBox=\"0 -960 960 960\" width=\"24\"><path d=\"M561-240 320-481l241-241 43 43-198 198 198 198-43 43Z\"/></svg>");
		if (previous.isPresent())
			out.write("</a>");

		out.write("</td><td style=\"text-align:center;vertical-align:top;\">");
		if (resource.getEntry().size() > 0)
		{
			int page = getPage(resourceUri);
			int count = getCount(resourceUri);
			int max = (int) Math.ceil((double) resource.getTotal() / count);
			int firstResource = ((page - 1) * count) + 1;
			int lastResource = ((page - 1) * count) + resource.getEntry().size();
			out.write("<span id=\"resources\">Resources " + firstResource + " - " + lastResource + " / "
					+ resource.getTotal() + "</span><span id=\"page\">Page " + page + " / " + max + "</span>");
		}
		out.write("</td><td style=\"text-align:right;\">");

		Optional<String> next = resource.getLink().stream().filter(l -> "next".equals(l.getRelation())).findFirst()
				.map(BundleLinkComponent::getUrl);
		if (next.isPresent())
			out.write("<a href=\"" + next.get() + "\" title=\"Next Page\">");
		out.write("<svg class=\"icon\" " + (next.isEmpty() ? " disabled" : "")
				+ " height=\"24\" viewBox=\"0 -960 960 960\" width=\"24\"><path d=\"M530-481 332-679l43-43 241 241-241 241-43-43 198-198Z\"/></svg>");
		if (next.isPresent())
			out.write("</a>");

		Optional<String> last = resource.getLink().stream().filter(l -> "last".equals(l.getRelation())).findFirst()
				.map(BundleLinkComponent::getUrl);
		if (last.isPresent())
			out.write("<a href=\"" + last.get() + "\" title=\"Last Page\">");
		out.write("<svg class=\"icon\" " + (last.isEmpty() ? " disabled" : "")
				+ " height=\"24\" viewBox=\"0 -960 960 960\" width=\"24\"><path d=\"m272-245-43-43 192-192-192-192 43-43 235 235-235 235Zm388 5v-480h60v480h-60Z\"/></svg>");
		if (last.isPresent())
			out.write("</a>");

		out.write("</td></tr></table></div>");

		out.write("<div id=\"list\"><table>");
		out.write(getHeader(resourceUri));
		out.write(resource.getEntry().stream()
				.filter(e -> e.hasResource() && e.hasSearch() && e.getSearch().hasMode()
						&& SearchEntryMode.MATCH.equals(e.getSearch().getMode()))
				.map(BundleEntryComponent::getResource)
				.map(r -> "<tr onClick=\"if(event.target?.tagName?.toLowerCase() !== 'a') window.location=document.head.baseURI + '"
						+ r.getIdElement().toVersionless().getValueAsString() + "'\" title=\"Open "
						+ r.getResourceType().name() + "\">" + getRow(r) + "</tr>\n")
				.collect(Collectors.joining()));
		out.write("</table></div>");

		long includeResources = resource.getEntry().stream().filter(
				e -> e.hasResource() && e.hasSearch() && SearchEntryMode.INCLUDE.equals(e.getSearch().getMode()))
				.count();
		if (includeResources > 0)
			out.write("<div id=\"footer\"><p style=\"font-style: italic;\">" + includeResources + " include "
					+ (includeResources == 1 ? "resource" : "resources") + " hidden.</div>");

		List<String> diagnostics = resource.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).filter(r -> r instanceof OperationOutcome)
				.map(r -> (OperationOutcome) r).map(OperationOutcome::getIssue).flatMap(List::stream)
				.filter(OperationOutcomeIssueComponent::hasSeverity)
				.filter(OperationOutcomeIssueComponent::hasDiagnostics)
				.map(i -> i.getSeverity().getDisplay() + ": " + i.getDiagnostics()).toList();
		for (String diag : diagnostics)
			out.write("<div id=\"footer\"><p style=\"font-style: italic;\">" + HtmlUtils.htmlEscape(diag) + "</div>");

		out.write("</div>");
	}

	private int getPage(URI uri)
	{
		MultivaluedMap<String, String> params = UriComponent.decodeQuery(uri, false);
		String p = params.getFirst("_page");
		if (p != null && !p.isBlank() && p.matches("-{0,1}[0-9]+"))
			return Integer.parseInt(p);
		else
			return 1;
	}

	private int getCount(URI uri)
	{
		MultivaluedMap<String, String> params = UriComponent.decodeQuery(uri, false);
		String p = params.getFirst("_count");
		if (p != null && !p.isBlank() && p.matches("-{0,1}[0-9]+"))
			return Integer.parseInt(p);
		else
			return defaultPageCount;
	}

	private String getHeader(URI uri)
	{
		if (taskRessourcePath.equals(uri.getPath()))
			return getTaskHeader();
		else if (questionnaireResponseRessourcePath.equals(uri.getPath()))
			return getQuestionnaireResponseHeader();
		else if (organizationResourcePath.equals(uri.getPath()))
			return getOrganizationHeader();
		else if (organizationAffiliationResourcePath.equals(uri.getPath()))
			return getOrganizationAffiliationHeader();
		else if (endpointResourcePath.equals(uri.getPath()))
			return getEndpointHeader();
		else
			throw new IllegalArgumentException("Unexpected resource path: " + uri.getPath());
	}

	private String getTaskHeader()
	{
		return "<tr><th>ID</th><th>Status</th><th>Process</th><th>Message-Name</th><th>Requester</th><th>Business-Key / Identifier</th><th>Last Updated</th></tr>";
	}

	private String getQuestionnaireResponseHeader()
	{
		return "<tr><th>ID</th><th>Status</th><th>Questionnaire</th><th>Business-Key</th><th>Last Updated</th></tr>";
	}

	private String getOrganizationHeader()
	{
		return "<tr><th>ID</th><th>Active</th><th>Identifier</th><th>Name</th><th>Endpoint</th><th>Last Updated</th></tr>";
	}

	private String getOrganizationAffiliationHeader()
	{
		return "<tr><th>ID</th><th>Active</th><th>Parent Organization</th><th>Participating Organization</th><th>Role</th><th>Endpoint</th><th>Last Updated</th></tr>";
	}

	private String getEndpointHeader()
	{
		return "<tr><th>ID</th><th>Status</th><th>Identifier</th><th>Name</th><th>Address</th><th>Managing Organization</th><th>Last Updated</th></tr>";
	}

	private String getRow(Resource resource)
	{
		if (resource instanceof Task t)
			return getTaskRow(t);
		else if (resource instanceof QuestionnaireResponse qr)
			return getQuestionnaireResponseRow(qr);
		else if (resource instanceof Organization o)
			return getOrganizationRow(o);
		else if (resource instanceof OrganizationAffiliation oa)
			return getOrganizationAffiliationRow(oa);
		else if (resource instanceof Endpoint e)
			return getEndpointRow(e);
		else
			throw new IllegalArgumentException("Unexpected resource type: " + resource.getResourceType().name());
	}

	private String getResourceType(IIdType id)
	{
		return id != null ? id.getResourceType() : "";
	}

	private String createResourceLink(Resource resource)
	{
		if (resource == null || !resource.hasIdElement() || !resource.getIdElement().hasIdPart())
			return "";
		else
			return "<a href=\"" + resource.getResourceType().name() + "/" + resource.getIdElement().getIdPart()
					+ "\" title=\"Open " + getResourceType(resource.getIdElement()) + "\">"
					+ resource.getIdElement().getIdPart() + "</a>";
	}

	private String createResourceLink(List<? extends Reference> references)
	{
		if (references == null || references.isEmpty())
			return "";
		else
		{
			// TODO maybe filter references to external servers (IdType.baseUrl not null and not this servers baseUrl)
			List<IIdType> filteredReferences = references.stream().filter(Reference::hasReference)
					.map(Reference::getReferenceElement).filter(IIdType::hasValue).toList();

			if (filteredReferences.isEmpty())
				return "";

			return "<a href=\"" + filteredReferences.get(0).toVersionless().getValue() + "\" title=\"Open "
					+ getResourceType(filteredReferences.get(0)) + "\">" + filteredReferences.get(0).getIdPart()
					+ "</a>" + (filteredReferences.size() > 1 ? ", ..." : "");
		}
	}

	private String createResourceLink(Reference reference)
	{
		// TODO maybe filter reference to external server (IdType.baseUrl not null and not this servers baseUrl)
		if (reference == null || !reference.hasReferenceElement() || !reference.getReferenceElement().hasValue())
			return "";
		else
		{
			return "<a href=\"" + reference.getReferenceElement().toVersionless().getValue() + "\" title=\"Open "
					+ getResourceType(reference.getReferenceElement()) + "\">"
					+ reference.getReferenceElement().toVersionless().getIdPart() + "</a>";
		}
	}

	private String toLastUpdated(Resource resource)
	{
		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasLastUpdated())
			return "";
		else
			return DATE_TIME_DISPLAY_FORMAT.format(resource.getMeta().getLastUpdated());
	}

	private String getTaskRow(Task resource)
	{
		String domain = "", processName = "", processVersion = "";
		if (resource.getInstantiatesCanonical() != null && !resource.getInstantiatesCanonical().isBlank())
		{
			Matcher matcher = INSTANTIATES_CANONICAL_PATTERN.matcher(resource.getInstantiatesCanonical());
			if (matcher.matches())
			{
				domain = matcher.group("domain");
				processName = matcher.group("processName");
				processVersion = matcher.group("processVersion");
			}
		}

		String businessKeyOrIdentifier = TaskStatus.DRAFT.equals(resource.getStatus()) ? resource.getIdentifier()
				.stream()
				.filter(i -> i.hasSystem() && NAMING_SYSTEM_TASK_IDENTIFIER.equals(i.getSystem()) && i.hasValue())
				.map(Identifier::getValue).findFirst().map(v ->
				{
					String[] parts = v.split("/");
					return parts.length > 0 ? parts[parts.length - 1] : "";
				}).orElse("")
				: resource.getInput().stream()
						.filter(isStringParam(CODE_SYSTEM_BPMN_MESSAGE, CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY))
						.findFirst().map(c -> ((StringType) c.getValue()).getValue()).orElse("");

		String messageName = resource.getInput().stream()
				.filter(isStringParam(CODE_SYSTEM_BPMN_MESSAGE, CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME)).findFirst()
				.map(c -> ((StringType) c.getValue()).getValue()).orElse("");

		return "<td status=\"" + (resource.hasStatus() ? resource.getStatus().toCode() : "") + "\" class=\"id-value\">"
				+ createResourceLink(resource) + "</td><td>"
				+ (resource.hasStatus() ? resource.getStatus().toCode() : "") + "</td><td>" + domain + " | "
				+ processName + " | " + processVersion + "</td><td>" + messageName + "</td><td>"
				+ (resource.hasRequester() && resource.getRequester().hasIdentifier()
						&& resource.getRequester().getIdentifier().hasValue()
								? resource.getRequester().getIdentifier().getValue()
								: "")
				+ "</td><td " + (TaskStatus.DRAFT.equals(resource.getStatus()) ? "" : "class=\"id-value\"") + ">"
				+ businessKeyOrIdentifier + "</td><td>" + toLastUpdated(resource) + "</td>";
	}

	private Predicate<ParameterComponent> isStringParam(String system, String code)
	{
		return p -> p.hasType() && p.getType().hasCoding()
				&& p.getType().getCoding().stream()
						.anyMatch(c -> system.equals(c.getSystem()) && code.equals(c.getCode()))
				&& p.hasValue() && p.getValue() instanceof StringType;
	}

	private String getQuestionnaireResponseRow(QuestionnaireResponse resource)
	{
		String businessKey = resource.getItem().stream()
				.filter(i -> "business-key".equals(i.getLinkId()) && i.hasAnswer() && i.getAnswer().size() == 1
						&& i.getAnswerFirstRep().hasValueStringType())
				.map(i -> i.getAnswerFirstRep().getValueStringType().getValue()).findFirst().orElse("");

		return "<td status=\"" + (resource.hasStatus() ? resource.getStatus().toCode() : "") + "\" class=\"id-value\">"
				+ createResourceLink(resource) + "</td><td>"
				+ (resource.hasStatus() ? resource.getStatus().toCode() : "") + "</td><td>"
				+ (resource.hasQuestionnaire() ? resource.getQuestionnaire().replaceAll("\\|", " \\| ") : "")
				+ "</td><td class=\"id-value\">" + businessKey + "</td><td>" + toLastUpdated(resource) + "</td>";
	}

	private <D extends DomainResource> String getIdentifierValues(D resource, Function<D, Boolean> hasIdentifier,
			Function<D, List<Identifier>> getIdentifier, String identifierSystem)
	{
		if (!hasIdentifier.apply(resource))
			return "";

		List<String> filteredIdentifiers = getIdentifier.apply(resource).stream()
				.filter(i -> identifierSystem.equals(i.getSystem())).filter(Identifier::hasValue)
				.map(Identifier::getValue).toList();

		if (filteredIdentifiers.isEmpty())
			return "";

		return filteredIdentifiers.get(0) + (filteredIdentifiers.size() > 1 ? ", ..." : "");
	}

	private String getOrganizationRow(Organization resource)
	{
		String identifier = getIdentifierValues(resource, Organization::hasIdentifier, Organization::getIdentifier,
				NAMING_SYSTEM_ORGANIZATION_IDENTIFIER);
		String name = resource.hasName() ? resource.getName() : "";

		return "<td class=\"id-value\" active=\"" + (resource.hasActive() ? resource.getActive() : "") + "\">"
				+ createResourceLink(resource) + "</td><td>" + (resource.hasActive() ? resource.getActive() : "")
				+ "</td><td>" + identifier + "</td><td>" + name + "</td><td class=\"id-value\">"
				+ createResourceLink(resource.getEndpoint()) + "</td><td>" + toLastUpdated(resource) + "</td>";
	}

	private String getOrganizationAffiliationRow(OrganizationAffiliation resource)
	{
		String role = resource.getCode().stream().flatMap(c -> c.getCoding().stream())
				.filter(c -> CODE_SYSTEM_ORGANIZATION_ROLE.equals(c.getSystem())).map(Coding::getCode)
				.collect(Collectors.joining(", "));

		return "<td class=\"id-value\" active=\"" + (resource.hasActive() ? resource.getActive() : "") + "\">"
				+ createResourceLink(resource) + "</td><td>" + (resource.hasActive() ? resource.getActive() : "")
				+ "</td><td class=\"id-value\">" + createResourceLink(resource.getOrganization())
				+ "</td><td class=\"id-value\">" + createResourceLink(resource.getParticipatingOrganization())
				+ "</td><td>" + role + "</td><td class=\"id-value\">" + createResourceLink(resource.getEndpoint())
				+ "</td><td>" + toLastUpdated(resource) + "</td>";
	}

	private String getEndpointRow(Endpoint resource)
	{
		String identifier = getIdentifierValues(resource, Endpoint::hasIdentifier, Endpoint::getIdentifier,
				NAMING_SYSTEM_ENDPOINT_IDENTIFIER);
		String name = resource.hasName() ? resource.getName() : "";

		return "<td class=\"id-value\" status=\"" + (resource.hasStatus() ? resource.getStatus().toCode() : "") + "\">"
				+ createResourceLink(resource) + "</td><td>"
				+ (resource.hasStatus() ? resource.getStatus().toCode() : "") + "</td><td>" + identifier + "</td><td>"
				+ name + "</td><td>" + (resource.hasAddress() ? resource.getAddress() : "")
				+ "</td><td class=\"id-value\">" + createResourceLink(resource.getManagingOrganization()) + "</td><td>"
				+ toLastUpdated(resource) + "</td>";
	}
}
