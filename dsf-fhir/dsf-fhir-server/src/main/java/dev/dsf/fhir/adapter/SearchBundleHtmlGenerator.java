package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.glassfish.jersey.uri.UriComponent;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
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
		out.write(
				resource.getEntry().stream()
						.filter(e -> e.hasResource() && e.hasSearch() && e.getSearch().hasMode()
								&& SearchEntryMode.MATCH.equals(e.getSearch().getMode()))
						.map(BundleEntryComponent::getResource)
						.map(r -> "<tr onClick=\"window.location=document.head.baseURI+'"
								+ r.getIdElement().toVersionless().getValueAsString() + "'\" title=\"Open Resource\">"
								+ getRow(r) + "</tr>\n")
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
		return "<tr><th>ID</th><th>Status</th><th>Process</th><th>Message-Name</th><th>Requester</th><th>Business-Key</th><th>Last Updated</th></tr>";
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
		if (resource instanceof Task)
			return getTaskRow((Task) resource);
		else if (resource instanceof QuestionnaireResponse)
			return getQuestionnaireResponseRow((QuestionnaireResponse) resource);
		else if (resource instanceof Organization)
			return getOrganizationRow((Organization) resource);
		else if (resource instanceof OrganizationAffiliation)
			return getOrganizationAffiliationRow((OrganizationAffiliation) resource);
		else if (resource instanceof Endpoint)
			return getEndpointRow((Endpoint) resource);
		else
			throw new IllegalArgumentException("Unexpected resource type: " + resource.getResourceType().name());
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

		String businessKey = resource.getInput().stream()
				.filter(isStringParam(CODE_SYSTEM_BPMN_MESSAGE, CODE_SYSTEM_BPMN_MESSAGE_BUSINESS_KEY)).findFirst()
				.map(c -> ((StringType) c.getValue()).getValue()).orElse("");

		String messageName = resource.getInput().stream()
				.filter(isStringParam(CODE_SYSTEM_BPMN_MESSAGE, CODE_SYSTEM_BPMN_MESSAGE_MESSAGE_NAME)).findFirst()
				.map(c -> ((StringType) c.getValue()).getValue()).orElse("");

		return "<td status=\"" + resource.getStatus().toCode() + "\" class=\"id-value\">"
				+ resource.getIdElement().getIdPart() + "</td><td>" + resource.getStatus().toCode() + "</td><td>"
				+ domain + " | " + processName + " | " + processVersion + "</td><td>" + messageName + "</td><td>"
				+ resource.getRequester().getIdentifier().getValue() + "</td><td class=\"id-value\">" + businessKey
				+ "</td><td>" + DATE_TIME_DISPLAY_FORMAT.format(resource.getMeta().getLastUpdated()) + "</td>";
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
		String id = resource.getIdElement().getIdPart();
		String idHref = questionnaireResponseRessourcePath + "/" + id;

		String businessKey = resource.getItem().stream()
				.filter(i -> "business-key".equals(i.getLinkId()) && i.hasAnswer() && i.getAnswer().size() == 1
						&& i.getAnswerFirstRep().hasValueStringType())
				.map(i -> i.getAnswerFirstRep().getValueStringType().getValue()).findFirst().orElse("");

		return "<td class=\"id-value\" status=\"" + resource.getStatus().toCode() + "\"><a href=\"" + idHref + "\">"
				+ id + "</a></td><td>" + resource.getStatus().toCode() + "</td><td>"
				+ resource.getQuestionnaire().replaceAll("\\|", " \\| ") + "</td><td class=\"id-value\">" + businessKey
				+ "</td><td>" + DATE_TIME_DISPLAY_FORMAT.format(resource.getMeta().getLastUpdated()) + "</td>";
	}

	private String getOrganizationRow(Organization resource)
	{
		String id = resource.getIdElement().getIdPart();
		String idHref = organizationResourcePath + "/" + id;

		String identifier = "";
		if (resource.hasIdentifier())
		{
			identifier = resource.getIdentifier().size() > 1 ? resource.getIdentifierFirstRep().getValue() + ", ..."
					: resource.getIdentifierFirstRep().getValue();
		}

		String name = resource.getName() != null ? resource.getName() : "";

		String endpointLink = "";
		if (resource.hasEndpoint())
		{
			String endpointId = resource.getEndpointFirstRep().getReferenceElement().getIdPart();
			String endpointHref = endpointResourcePath + "/" + endpointId;
			endpointLink = "<a href=\"" + endpointHref + "\">" + endpointId + "</a>"
					+ (resource.getEndpoint().size() > 1 ? ", ..." : "");
		}

		return "<td class=\"id-value\" active=\"" + resource.getActive() + "\"><a href=\"" + idHref + "\">" + id
				+ "</a></td><td>" + resource.getActive() + "</td><td>" + identifier + "</td><td>" + name
				+ "</td><td class=\"id-value\">" + endpointLink + "</td><td>"
				+ DATE_TIME_DISPLAY_FORMAT.format(resource.getMeta().getLastUpdated()) + "</td>";
	}

	private String getOrganizationAffiliationRow(OrganizationAffiliation resource)
	{
		String id = resource.getIdElement().getIdPart();
		String idHref = organizationResourcePath + "/" + id;

		String parentOrganizationLink = "";
		if (resource.hasOrganization())
		{
			String parentOrganizationId = resource.getOrganization().getReferenceElement().getIdPart();
			String parentOrganizationHref = organizationResourcePath + "/" + parentOrganizationId;
			parentOrganizationLink = "<a href=\"" + parentOrganizationHref + "\">" + parentOrganizationId + "</a>";
		}

		String participatingOrganizationLink = "";
		if (resource.hasParticipatingOrganization())
		{
			String participatingOrganizationId = resource.getParticipatingOrganization().getReferenceElement()
					.getIdPart();
			String participatingOrganizationHref = organizationResourcePath + "/" + participatingOrganizationId;
			participatingOrganizationLink = "<a href=\"" + participatingOrganizationHref + "\">"
					+ participatingOrganizationId + "</a>";
		}

		String role = resource.getCode().stream().flatMap(c -> c.getCoding().stream())
				.filter(c -> CODE_SYSTEM_ORGANIZATION_ROLE.equals(c.getSystem())).map(Coding::getCode)
				.collect(Collectors.joining(", "));

		String endpointLink = "";
		if (resource.hasEndpoint())
		{
			String endpointId = resource.getEndpointFirstRep().getReferenceElement().getIdPart();
			String endpointHref = endpointResourcePath + "/" + endpointId;
			endpointLink = "<a href=\"" + endpointHref + "\">" + endpointId + "</a>"
					+ (resource.getEndpoint().size() > 1 ? ", ..." : "");
		}

		return "<td class=\"id-value\" active=\"" + resource.getActive() + "\"><a href=\"" + idHref + "\">" + id
				+ "</a></td><td>" + resource.getActive() + "</td><td class=\"id-value\">" + parentOrganizationLink
				+ "</td><td class=\"id-value\">" + participatingOrganizationLink + "</td><td>" + role
				+ "</td><td class=\"id-value\">" + endpointLink + "</td><td>"
				+ DATE_TIME_DISPLAY_FORMAT.format(resource.getMeta().getLastUpdated()) + "</td>";
	}

	private String getEndpointRow(Endpoint resource)
	{
		String id = resource.getIdElement().getIdPart();
		String idHref = organizationResourcePath + "/" + id;

		String identifier = "";
		if (resource.hasIdentifier())
		{
			identifier = resource.getIdentifier().size() > 1 ? resource.getIdentifierFirstRep().getValue() + ", ..."
					: resource.getIdentifierFirstRep().getValue();
		}

		String name = resource.getName() != null ? resource.getName() : "";

		String managingOrganizationLink = "";
		if (resource.hasManagingOrganization())
		{
			String managingOrganizationId = resource.getManagingOrganization().getReferenceElement().getIdPart();
			String managingOrganizationHref = organizationResourcePath + "/" + managingOrganizationId;
			managingOrganizationLink = "<a href=\"" + managingOrganizationHref + "\">" + managingOrganizationId
					+ "</a>";
		}

		return "<td class=\"id-value\" status=\"" + resource.getStatus().toCode() + "\"><a href=\"" + idHref + "\">"
				+ id + "</a></td><td>" + resource.getStatus().toCode() + "</td><td>" + identifier + "</td><td>" + name
				+ "</td><td>" + resource.getAddress() + "</td><td class=\"id-value\">" + managingOrganizationLink
				+ "</td><td>" + DATE_TIME_DISPLAY_FORMAT.format(resource.getMeta().getLastUpdated()) + "</td>";
	}
}
