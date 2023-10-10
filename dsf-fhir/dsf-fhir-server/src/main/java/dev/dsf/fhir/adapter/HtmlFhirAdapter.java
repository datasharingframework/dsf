package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;
import org.springframework.web.util.HtmlUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.common.auth.conf.Identity;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.TEXT_HTML)
public class HtmlFhirAdapter extends AbstractAdapter implements MessageBodyWriter<Resource>
{
	private static final String RESOURCE_NAMES = "Account|ActivityDefinition|AdverseEvent|AllergyIntolerance|Appointment|AppointmentResponse|AuditEvent|Basic|Binary"
			+ "|BiologicallyDerivedProduct|BodyStructure|Bundle|CapabilityStatement|CarePlan|CareTeam|CatalogEntry|ChargeItem|ChargeItemDefinition|Claim|ClaimResponse"
			+ "|ClinicalImpression|CodeSystem|Communication|CommunicationRequest|CompartmentDefinition|Composition|ConceptMap|Condition|Consent|Contract|Coverage"
			+ "|CoverageEligibilityRequest|CoverageEligibilityResponse|DetectedIssue|Device|DeviceDefinition|DeviceMetric|DeviceRequest|DeviceUseStatement"
			+ "|DiagnosticReport|DocumentManifest|DocumentReference|DomainResource|EffectEvidenceSynthesis|Encounter|Endpoint|EnrollmentRequest|EnrollmentResponse"
			+ "|EpisodeOfCare|EventDefinition|Evidence|EvidenceVariable|ExampleScenario|ExplanationOfBenefit|FamilyMemberHistory|Flag|Goal|GraphDefinition|Group"
			+ "|GuidanceResponse|HealthcareService|ImagingStudy|Immunization|ImmunizationEvaluation|ImmunizationRecommendation|ImplementationGuide|InsurancePlan"
			+ "|Invoice|Library|Linkage|List|Location|Measure|MeasureReport|Media|Medication|MedicationAdministration|MedicationDispense|MedicationKnowledge"
			+ "|MedicationRequest|MedicationStatement|MedicinalProduct|MedicinalProductAuthorization|MedicinalProductContraindication|MedicinalProductIndication"
			+ "|MedicinalProductIngredient|MedicinalProductInteraction|MedicinalProductManufactured|MedicinalProductPackaged|MedicinalProductPharmaceutical"
			+ "|MedicinalProductUndesirableEffect|MessageDefinition|MessageHeader|MolecularSequence|NamingSystem|NutritionOrder|Observation|ObservationDefinition"
			+ "|OperationDefinition|OperationOutcome|Organization|OrganizationAffiliation|Parameters|Patient|PaymentNotice|PaymentReconciliation|Person|PlanDefinition"
			+ "|Practitioner|PractitionerRole|Procedure|Provenance|Questionnaire|QuestionnaireResponse|RelatedPerson|RequestGroup|ResearchDefinition"
			+ "|ResearchElementDefinition|ResearchStudy|ResearchSubject|Resource|RiskAssessment|RiskEvidenceSynthesis|Schedule|SearchParameter|ServiceRequest|Slot"
			+ "|Specimen|SpecimenDefinition|StructureDefinition|StructureMap|Subscription|Substance|SubstanceNucleicAcid|SubstancePolymer|SubstanceProtein"
			+ "|SubstanceReferenceInformation|SubstanceSourceMaterial|SubstanceSpecification|SupplyDelivery|SupplyRequest|Task|TerminologyCapabilities|TestReport"
			+ "|TestScript|ValueSet|VerificationResult|VisionPrescription";

	private static final String UUID = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

	private static final Pattern URL_PATTERN = Pattern
			.compile("(https?)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_.|]");
	private static final Pattern XML_REFERENCE_UUID_PATTERN = Pattern
			.compile("&lt;reference value=\"((" + RESOURCE_NAMES + ")/" + UUID + ")\"/&gt;");
	private static final Pattern JSON_REFERENCE_UUID_PATTERN = Pattern
			.compile("\"reference\": \"((" + RESOURCE_NAMES + ")/" + UUID + ")\",");
	private static final Pattern XML_ID_UUID_AND_VERSION_PATTERN = Pattern.compile(
			"&lt;id value=\"(" + UUID + ")\"/&gt;\\n([ ]*)&lt;meta&gt;\\n([ ]*)&lt;versionId value=\"([0-9]+)\"/&gt;");
	private static final Pattern JSON_ID_UUID_AND_VERSION_PATTERN = Pattern
			.compile("\"id\": \"(" + UUID + ")\",\\n([ ]*)\"meta\": \\{\\n([ ]*)\"versionId\": \"([0-9]+)\",");

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();

	private final String serverBaseUrl;
	private final FhirContext fhirContext;
	private final Map<Class<? extends Resource>, List<HtmlGenerator<? extends Resource>>> htmlGeneratorsByType;

	@Context
	private volatile UriInfo uriInfo;

	@Context
	private volatile SecurityContext securityContext;

	public HtmlFhirAdapter(String serverBaseUrl, FhirContext fhirContext,
			Collection<? extends HtmlGenerator<?>> htmlGenerators)
	{
		this.serverBaseUrl = serverBaseUrl;
		this.fhirContext = fhirContext;

		if (htmlGenerators != null)
			htmlGeneratorsByType = htmlGenerators.stream()
					.collect(Collectors.groupingBy(HtmlGenerator::getResourceType));
		else
			htmlGeneratorsByType = Collections.emptyMap();
	}

	private String getServerBaseUrlPathWithLeadingSlash()
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

	protected FhirContext getFhirContext()
	{
		return fhirContext;
	}

	@Override
	protected IParser getParser(MediaType mediaType, Supplier<IParser> parser)
	{
		IParser p = super.getParser(mediaType, parser);
		p.setPrettyPrint(true);
		return p;
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return type != null && Resource.class.isAssignableFrom(type);
	}

	@Override
	public void writeTo(Resource resource, Class<?> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException
	{
		final boolean htmlEnabled = isHtmlEnabled(type, resource);
		final OutputStreamWriter out = new OutputStreamWriter(entityStream);

		out.write("""
				<!DOCTYPE html>
				<html>
				<head>
				<base href="${serverBaseUrl}/">
				<link rel="icon" type="image/svg+xml" href="static/favicon.svg">
				<link rel="icon" type="image/png" href="static/favicon_32x32.png" sizes="32x32">
				<link rel="icon" type="image/png" href="static/favicon_96x96.png" sizes="96x96">
				<meta name="theme-color" content="#326F95">
				<script src="static/util.js"></script>
				<script src="static/prettify.js"></script>
				<script src="static/tabs.js"></script>
				<script src="static/bookmarks.js"></script>
				<script src="static/help.js"></script>
				<script src="static/form.js"></script>
				<link rel="stylesheet" type="text/css" href="static/prettify.css">
				<link rel="stylesheet" type="text/css" href="static/dsf.css">
				<link rel="stylesheet" type="text/css" href="static/form.css">
				""".replace("${serverBaseUrl}", getServerBaseUrlPathWithLeadingSlash()));
		out.write("<title>" + getTitle() + "</title>\n");
		out.write("</head>\n");
		out.write("<body onload=\"prettyPrint();openInitialTab(" + String.valueOf(htmlEnabled) + ");checkBookmarked();"
				+ adaptFormInputsIfTask(resource) + "setUiTheme();\">\n");

		out.write("<div id=\"icons\">\n");

		Principal userPrincipal = securityContext.getUserPrincipal();
		if (userPrincipal instanceof Identity)
		{
			Identity identity = (Identity) userPrincipal;
			out.write("<span id=\"hello-user\">");
			out.write("Hello, ");
			out.write(identity.getDisplayName());
			out.write("</span>\n");
		}

		if ("OPENID".equals(securityContext.getAuthenticationScheme()))
		{
			out.write(
					"""
							<a href="logout">
							<svg class="icon" id="logout-icon" viewBox="0 0 24 24">
							<title>Logout</title>
							<path d="M5 21q-.825 0-1.413-.587Q3 19.825 3 19V5q0-.825.587-1.413Q4.175 3 5 3h7v2H5v14h7v2Zm11-4-1.375-1.45 2.55-2.55H9v-2h8.175l-2.55-2.55L16 7l5 5Z"/>
							</svg></a>
							""");
		}

		out.write(
				"""
						<svg class="icon" id="help-icon" viewBox="0 0 24 24" onclick="showHelp();">
						<title>Show Help</title>
						<path d="M11.07,12.85c0.77-1.39,2.25-2.21,3.11-3.44c0.91-1.29,0.4-3.7-2.18-3.7c-1.69,0-2.52,1.28-2.87,2.34L6.54,6.96 C7.25,4.83,9.18,3,11.99,3c2.35,0,3.96,1.07,4.78,2.41c0.7,1.15,1.11,3.3,0.03,4.9c-1.2,1.77-2.35,2.31-2.97,3.45 c-0.25,0.46-0.35,0.76-0.35,2.24h-2.89C10.58,15.22,10.46,13.95,11.07,12.85z M14,20c0,1.1-0.9,2-2,2s-2-0.9-2-2c0-1.1,0.9-2,2-2 S14,18.9,14,20z"/>
						</svg>
						<svg class="icon" id="light-mode" height="24" viewBox="0 -960 960 960" width="24" onclick="setUiTheme('light');">
						<title>Enable Light Mode</title>
						<path d="M479.765-340Q538-340 579-380.765q41-40.764 41-99Q620-538 579.235-579q-40.764-41-99-41Q422-620 381-579.235q-41 40.764-41 99Q340-422 380.765-381q40.764 41 99 41Zm.235 60q-83 0-141.5-58.5T280-480q0-83 58.5-141.5T480-680q83 0 141.5 58.5T680-480q0 83-58.5 141.5T480-280ZM70-450q-12.75 0-21.375-8.675Q40-467.351 40-480.175 40-493 48.625-501.5T70-510h100q12.75 0 21.375 8.675 8.625 8.676 8.625 21.5 0 12.825-8.625 21.325T170-450H70Zm720 0q-12.75 0-21.375-8.675-8.625-8.676-8.625-21.5 0-12.825 8.625-21.325T790-510h100q12.75 0 21.375 8.675 8.625 8.676 8.625 21.5 0 12.825-8.625 21.325T890-450H790ZM479.825-760Q467-760 458.5-768.625T450-790v-100q0-12.75 8.675-21.375 8.676-8.625 21.5-8.625 12.825 0 21.325 8.625T510-890v100q0 12.75-8.675 21.375-8.676 8.625-21.5 8.625Zm0 720Q467-40 458.5-48.625T450-70v-100q0-12.75 8.675-21.375 8.676-8.625 21.5-8.625 12.825 0 21.325 8.625T510-170v100q0 12.75-8.675 21.375Q492.649-40 479.825-40ZM240-678l-57-56q-9-9-8.629-21.603.37-12.604 8.526-21.5 8.896-8.897 21.5-8.897Q217-786 226-777l56 57q8 9 8 21t-8 20.5q-8 8.5-20.5 8.5t-21.5-8Zm494 495-56-57q-8-9-8-21.375T678.5-282q8.5-9 20.5-9t21 9l57 56q9 9 8.629 21.603-.37 12.604-8.526 21.5-8.896 8.897-21.5 8.897Q743-174 734-183Zm-56-495q-9-9-9-21t9-21l56-57q9-9 21.603-8.629 12.604.37 21.5 8.526 8.897 8.896 8.897 21.5Q786-743 777-734l-57 56q-8 8-20.364 8-12.363 0-21.636-8ZM182.897-182.897q-8.897-8.896-8.897-21.5Q174-217 183-226l57-56q8.8-9 20.9-9 12.1 0 20.709 9Q291-273 291-261t-9 21l-56 57q-9 9-21.603 8.629-12.604-.37-21.5-8.526ZM480-480Z"/>
						</svg>
						<svg class="icon" id="dark-mode" height="24" viewBox="0 -960 960 960" width="24" onclick="setUiTheme('dark');">
						<title>Enable Dark Mode</title>
						<path d="M480-120q-150 0-255-105T120-480q0-150 105-255t255-105q8 0 17 .5t23 1.5q-36 32-56 79t-20 99q0 90 63 153t153 63q52 0 99-18.5t79-51.5q1 12 1.5 19.5t.5 14.5q0 150-105 255T480-120Zm0-60q109 0 190-67.5T771-406q-25 11-53.667 16.5Q688.667-384 660-384q-114.689 0-195.345-80.655Q384-545.311 384-660q0-24 5-51.5t18-62.5q-98 27-162.5 109.5T180-480q0 125 87.5 212.5T480-180Zm-4-297Z"/>
						</svg>
						<a href="" download="" id="download-link" title="">
						<svg class="icon" id="download" viewBox="0 0 24 24">
						<path d="M18,15v3H6v-3H4v3c0,1.1,0.9,2,2,2h12c1.1,0,2-0.9,2-2v-3H18z M17,11l-1.41-1.41L13,12.17V4h-2v8.17L8.41,9.59L7,11l5,5 L17,11z"/>
						</svg></a>
						<svg class="icon" id="bookmark-add" viewBox="0 0 24 24" onclick="addCurrentBookmark();">
						<title>Add Bookmark</title>
						<path d="M17,11v6.97l-5-2.14l-5,2.14V5h6V3H7C5.9,3,5,3.9,5,5v16l7-3l7,3V11H17z M21,7h-2v2h-2V7h-2V5h2V3h2v2h2V7z"/>
						</svg>
						<svg class="icon" id="bookmark-remove" viewBox="0 0 24 24" onclick="removeCurrentBookmark();" style="display:none;">
						<title>Remove Bookmark</title>
						<path d="M17,11v6.97l-5-2.14l-5,2.14V5h6V3H7C5.9,3,5,3.9,5,5v16l7-3l7,3V11H17z M21,7h-6V5h6V7z"/>
						</svg>
						<svg class="icon" id="bookmark-list" viewBox="0 0 24 24" onclick="showBookmarks();">
						<title>Show Bookmarks</title>
						<path d="M9,1H19A2,2 0 0,1 21,3V19L19,18.13V3H7A2,2 0 0,1 9,1M15,20V7H5V20L10,17.82L15,20M15,5C16.11,5 17,5.9 17,7V23L10,20L3,23V7A2,2 0 0,1 5,5H15Z"/>
						</svg>
						</div>
						<div id="help" style="display:none;">
						<h3 id="help-title">Query Parameters</h3>
						<svg class="icon" id="help-close" viewBox="0 0 24 24" onclick="closeHelp();">
						<title>Close Help</title>
						<path fill="currentColor" d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
						</svg>
						<div id="help-list"></div>
						</div>
						<div id="bookmarks" style="display:none;">
						<h3 id="bookmarks-title">Bookmarks</h3>
						<svg class="icon" id="bookmark-list-close" viewBox="0 0 24 24" onclick="closeBookmarks();">
						<title>Close Bookmarks</title>
						<path fill="currentColor" d="M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12L19,6.41Z"/>
						</svg>
						<div id="bookmarks-list"></div>
						</div>
						<table id="header">
						<tr>
						<td>
						<image src="static/logo.svg">
						</td>
						<td id="url">
						<h1>
						""");
		out.write(getUrlHeading(resource));
		out.write("""
				</h1>
				</td>
				</tr>
				</table>
				<div class="tab">
				""");

		if (htmlEnabled)
			out.write("<button id=\"html-button\" class=\"tablinks\" onclick=\"openTab('html')\">html</button>\n");

		out.write("""
				<button id="json-button" class="tablinks" onclick="openTab('json')">json</button>
				<button id="xml-button" class="tablinks" onclick="openTab('xml')">xml</button>
				</div>
				""");

		writeXml(mediaType, resource, out);
		writeJson(mediaType, resource, out);

		if (htmlEnabled)
			writeHtml(type, resource, out);

		out.write("</html>");
		out.flush();
	}

	private String getTitle()
	{
		if (uriInfo == null || uriInfo.getPath() == null || uriInfo.getPath().isBlank())
			return "DSF";
		else if (uriInfo.getPath().endsWith("/"))
			return "DSF: " + HtmlUtils.htmlEscape(uriInfo.getPath().substring(0, uriInfo.getPath().length() - 1));
		else
			return "DSF: " + HtmlUtils.htmlEscape(uriInfo.getPath());
	}

	private String getUrlHeading(Resource resource) throws MalformedURLException
	{
		URI uri = getResourceUri(resource);
		String[] pathSegments = uri.getPath().split("/");

		String u = serverBaseUrl;
		StringBuilder heading = new StringBuilder("<a href=\"" + u + "/\" title=\"Open " + u + "\">" + u + "</a>");

		String[] basePathSegments = getServerBaseUrlPathWithLeadingSlash().split("/");
		for (int i = basePathSegments.length; i < pathSegments.length; i++)
		{
			String pathSegment = HtmlUtils.htmlEscape(pathSegments[i]);
			u += "/" + pathSegment;
			heading.append("<a href=\"" + u + "\" title=\"Open " + u + "\">/" + pathSegment + "</a>");
		}

		if (uri.getQuery() != null)
		{
			String queryValue = HtmlUtils.htmlEscape(uri.getQuery());
			u += "?" + queryValue;
			heading.append("<a href=\"" + u + "\" title=\"Open " + u + "\">?" + queryValue + "</a>");
		}
		else if (uriInfo.getQueryParameters().containsKey("_summary"))
		{
			String summaryValue = HtmlUtils.htmlEscape(uriInfo.getQueryParameters().getFirst("_summary"));
			u += "?_summary=" + summaryValue;
			heading.append("<a href=\"" + u + "\" title=\"Open " + u + "\">?_summary=" + summaryValue + "</a>");
		}

		heading.append('\n');

		return heading.toString();
	}


	private URI getResourceUri(Resource resource) throws MalformedURLException
	{
		return getResourceUrlString(resource).map(this::toURI).orElse(toURI(serverBaseUrl + "/" + uriInfo.getPath()));
	}

	private URI toURI(String str)
	{
		try
		{
			return new URI(str);
		}
		catch (URISyntaxException e)
		{
			throw new RuntimeException(e);
		}
	}

	private Optional<String> getResourceUrlString(Resource resource) throws MalformedURLException
	{
		if (resource instanceof Resource && resource.getIdElement().hasIdPart())
		{
			if (!uriInfo.getPath().contains("_history"))
				return Optional.of(String.format("%s/%s/%s", serverBaseUrl, resource.getIdElement().getResourceType(),
						resource.getIdElement().getIdPart()));
			else
				return Optional.of(
						String.format("%s/%s/%s/_history/%s", serverBaseUrl, resource.getIdElement().getResourceType(),
								resource.getIdElement().getIdPart(), resource.getIdElement().getVersionIdPart()));
		}
		else if (resource instanceof Bundle && !resource.getIdElement().hasIdPart())
			return ((Bundle) resource).getLink().stream().filter(c -> "self".equals(c.getRelation())).findFirst()
					.map(c -> c.getUrl());
		else
			return Optional.empty();
	}

	private void writeXml(MediaType mediaType, Resource resource, OutputStreamWriter out) throws IOException
	{
		IParser parser = getParser(mediaType, fhirContext::newXmlParser);

		out.write("<pre id=\"xml\" class=\"prettyprint linenums lang-xml\" style=\"display:none;\">");
		String content = parser.encodeResourceToString(resource);

		content = content.replace("&amp;", "&amp;amp;").replace("&apos;", "&amp;apos;").replace("&gt;", "&amp;gt;")
				.replace("&lt;", "&amp;lt;").replace("&quot;", "&amp;quot;");
		content = simplifyXml(content);
		content = content.replace("<", "&lt;").replace(">", "&gt;");

		Matcher versionMatcher = XML_ID_UUID_AND_VERSION_PATTERN.matcher(content);
		content = versionMatcher.replaceAll(result ->
		{
			Optional<String> resourceName = getResourceName(resource, result.group(1));
			return resourceName.map(rN -> "&lt;id value=\"<a href=\"" + rN + "/" + result.group(1) + "\">"
					+ result.group(1) + "</a>\"/&gt;\n" + result.group(2) + "&lt;meta&gt;\n" + result.group(3)
					+ "&lt;versionId value=\"" + "<a href=\"" + rN + "/" + result.group(1) + "/_history/"
					+ result.group(4) + "\">" + result.group(4) + "</a>" + "\"/&gt;").orElse(result.group(0));
		});

		Matcher urlMatcher = URL_PATTERN.matcher(content);
		content = urlMatcher.replaceAll(result -> "<a href=\""
				+ result.group().replace("&amp;amp;", "&amp;").replace("&amp;apos;", "&apos;")
						.replace("&amp;gt;", "&gt;").replace("&amp;lt;", "&lt;").replace("&amp;quot;", "&quot;")
				+ "\">" + result.group() + "</a>");

		Matcher referenceUuidMatcher = XML_REFERENCE_UUID_PATTERN.matcher(content);
		content = referenceUuidMatcher.replaceAll(
				result -> "&lt;reference value=\"<a href=\"" + result.group(1) + "\">" + result.group(1) + "</a>\"&gt");

		out.write(content);
		out.write("</pre>\n");
	}

	private Transformer newTransformer() throws TransformerConfigurationException
	{
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "3");
		return transformer;
	}

	private String simplifyXml(String xml)
	{
		try
		{
			Transformer transformer = newTransformer();
			StringWriter writer = new StringWriter();
			transformer.transform(new StreamSource(new StringReader(xml)), new StreamResult(writer));
			return writer.toString();
		}
		catch (TransformerException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void writeJson(MediaType mediaType, Resource resource, OutputStreamWriter out) throws IOException
	{
		IParser parser = getParser(mediaType, fhirContext::newJsonParser);

		out.write("<pre id=\"json\" class=\"prettyprint linenums lang-json\" style=\"display:none;\">");
		String content = parser.encodeResourceToString(resource).replace("<", "&lt;").replace(">", "&gt;");

		Matcher urlMatcher = URL_PATTERN.matcher(content);
		content = urlMatcher.replaceAll(result -> "<a href=\"" + result.group() + "\">" + result.group() + "</a>");

		Matcher referenceUuidMatcher = JSON_REFERENCE_UUID_PATTERN.matcher(content);
		content = referenceUuidMatcher.replaceAll(
				result -> "\"reference\": \"<a href=\"" + result.group(1) + "\">" + result.group(1) + "</a>\",");

		Matcher idUuidMatcher = JSON_ID_UUID_AND_VERSION_PATTERN.matcher(content);
		content = idUuidMatcher.replaceAll(result ->
		{
			Optional<String> resourceName = getResourceName(resource, result.group(1));
			return resourceName.map(rN -> "\"id\": \"<a href=\"" + rN + "/" + result.group(1) + "\">" + result.group(1)
					+ "</a>\",\n" + result.group(2) + "\"meta\": {\n" + result.group(3) + "\"versionId\": \""
					+ "<a href=\"" + rN + "/" + result.group(1) + "/_history/" + result.group(4) + "\">"
					+ result.group(4) + "</a>" + "\",").orElse(result.group(0));
		});

		out.write(content);
		out.write("</pre>\n");
	}

	@SuppressWarnings("unchecked")
	private void writeHtml(Class<?> resourceType, Resource resource, OutputStreamWriter out) throws IOException
	{
		out.write("<div id=\"html\" class=\"prettyprint lang-html\" style=\"display:none;\">\n");

		URI resourceUri = getResourceUri(resource);

		HtmlGenerator<Resource> generator = (HtmlGenerator<Resource>) htmlGeneratorsByType.get(resourceType).stream()
				.filter(g -> g.isResourceSupported(resourceUri, resource)).findFirst().get();
		generator.writeHtml(resourceUri, resource, out);

		out.write("</div>\n");
	}

	private boolean isHtmlEnabled(Class<?> resourceType, Resource resource) throws MalformedURLException
	{
		URI resourceUri = getResourceUri(resource);

		return htmlGeneratorsByType.containsKey(resourceType) && htmlGeneratorsByType.get(resourceType).stream()
				.anyMatch(g -> g.isResourceSupported(resourceUri, resource));
	}

	private String adaptFormInputsIfTask(Resource resource)
	{
		if (resource instanceof Task task)
			return Task.TaskStatus.DRAFT.equals(task.getStatus()) ? "adaptTaskFormInputs();" : "";
		else
			return "";
	}

	private Optional<String> getResourceName(Resource resource, String uuid)
	{
		if (resource instanceof Bundle)
		{
			// if persistent Bundle resource
			if (Objects.equals(uuid, resource.getIdElement().getIdPart()))
				return Optional.of(resource.getClass().getAnnotation(ResourceDef.class).name());
			else
				return ((Bundle) resource).getEntry().stream().filter(c ->
				{
					if (c.hasResource())
						return uuid.equals(c.getResource().getIdElement().getIdPart());
					else
						return uuid.equals(new IdType(c.getResponse().getLocation()).getIdPart());
				}).map(c ->
				{
					if (c.hasResource())
						return c.getResource().getClass().getAnnotation(ResourceDef.class).name();
					else
						return new IdType(c.getResponse().getLocation()).getResourceType();
				}).findFirst();
		}
		else if (resource instanceof Resource)
			return Optional.of(resource.getClass().getAnnotation(ResourceDef.class).name());
		else
			return Optional.empty();
	}
}
