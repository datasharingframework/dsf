package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.common.ui.theme.Theme;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

public class ThymeleafTemplateServiceImpl implements ThymeleafTemplateService, InitializingBean
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

	private static final String CODE_SYSTEM_PRACTITIONER_ROLE = "http://dsf.dev/fhir/CodeSystem/practitioner-role";

	private final String serverBaseUrl;
	private final Theme theme;
	private final FhirContext fhirContext;
	private final boolean modCssExists;

	private final Map<Class<? extends Resource>, List<ThymeleafContext>> contextsByResourceType;

	private final TransformerFactory transformerFactory = TransformerFactory.newInstance();
	private final TemplateEngine templateEngine = new TemplateEngine();

	/**
	 * @param serverBaseUrl
	 *            not <code>null</code>
	 * @param theme
	 *            may be <code>null</code>
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param contexts
	 *            may be <code>null</code>
	 * @param cacheEnabled
	 * @param modCssExists
	 */
	public ThymeleafTemplateServiceImpl(String serverBaseUrl, Theme theme, FhirContext fhirContext,
			List<? extends ThymeleafContext> contexts, boolean cacheEnabled, boolean modCssExists)
	{
		this.serverBaseUrl = serverBaseUrl;
		this.theme = theme;
		this.fhirContext = fhirContext;
		this.modCssExists = modCssExists;

		contextsByResourceType = contexts == null ? Map.of()
				: contexts.stream().collect(Collectors.groupingBy(ThymeleafContext::getResourceType));

		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setPrefix("/fhir/template/");
		resolver.setSuffix(".html");
		resolver.setCacheable(cacheEnabled);

		templateEngine.setTemplateResolver(resolver);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
		Objects.requireNonNull(fhirContext, "fhirContext");
		// theme may be null
	}

	@Override
	public void writeTo(Resource resource, Class<?> type, MediaType mediaType, UriInfo uriInfo,
			SecurityContext securityContext, OutputStream outputStream) throws IOException
	{
		Context context = new Context();
		context.setVariable("basePath", getServerBaseUrlPathWithLeadingSlash());
		context.setVariable("modCssExists", modCssExists);
		context.setVariable("theme", theme == null ? null : theme.toString());
		context.setVariable("title", getTitle(uriInfo));
		context.setVariable("heading", getHeading(resource, uriInfo));
		context.setVariable("username",
				securityContext.getUserPrincipal() instanceof Identity i ? i.getDisplayName() : null);

		String usernameTitle = "";
		if (securityContext.getUserPrincipal() instanceof PractitionerIdentity p)
		{
			if (p.getPractitionerIdentifierValue().isPresent())
				usernameTitle += "Mail: " + p.getPractitionerIdentifierValue().get();
			if (p.getPractitionerIdentifierValue().isPresent() && !p.getPractionerRoles().isEmpty())
				usernameTitle += " - ";
			if (!p.getPractionerRoles().isEmpty())
				usernameTitle += p.getPractionerRoles().stream()
						.map(c -> CODE_SYSTEM_PRACTITIONER_ROLE.equals(c.getSystem()) ? c.getCode()
								: c.getSystem() + "|" + c.getCode())
						.collect(Collectors.joining(", ", "Roles: ", ""));
		}
		context.setVariable("usernameTitle", usernameTitle);

		context.setVariable("practitionerIdentifierValue",
				securityContext.getUserPrincipal() instanceof PractitionerIdentity p
						? p.getPractitionerIdentifierValue().orElse(null)
						: null);

		context.setVariable("openid", "OPENID".equals(securityContext.getAuthenticationScheme()));
		context.setVariable("xml", toXml(mediaType, resource));
		context.setVariable("json", toJson(mediaType, resource));
		context.setVariable("resourceId", ElementId.from(resource));

		getContext(type, uriInfo, resource, securityContext.getUserPrincipal()).ifPresent(tContext ->
		{
			context.setVariable("htmlFragment", tContext.getHtmlFragment());
			tContext.setVariables(context::setVariable, resource);
		});

		OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
		templateEngine.process("main", context, writer);
	}

	private Optional<ThymeleafContext> getContext(Class<?> type, UriInfo uriInfo, Resource resource,
			Principal principal)
	{
		return contextsByResourceType.getOrDefault(type, List.of()).stream().filter(g ->
		{
			Optional<String> lastSegment = uriInfo.getPathSegments().stream().filter(Objects::nonNull)
					.map(PathSegment::getPath).filter(Objects::nonNull).filter(s -> !s.isBlank())
					.reduce((_, second) -> second);

			return lastSegment.map(g::isResourceSupported).orElseGet(() -> g.isRootSupported(resource, principal));
		}).findFirst();
	}

	private String getServerBaseUrlPathWithLeadingSlash()
	{
		try
		{
			return new URI(serverBaseUrl).getPath();
		}
		catch (URISyntaxException e)
		{
			throw new RuntimeException(e);
		}
	}

	private String getTitle(UriInfo uriInfo)
	{
		if (uriInfo == null || uriInfo.getPath() == null || uriInfo.getPath().isBlank())
			return "DSF";
		else if (uriInfo.getPath().endsWith("/"))
			return "DSF: " + HtmlUtils.htmlEscape(uriInfo.getPath().substring(0, uriInfo.getPath().length() - 1));
		else
			return "DSF: " + HtmlUtils.htmlEscape(uriInfo.getPath());
	}

	private String getHeading(Resource resource, UriInfo uriInfo)
	{
		URI uri = getResourceUri(resource, uriInfo);
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
			heading.append("<a href=\"" + u + "\" title=\"Open " + u + "\">?"
					+ queryValue.replace("&amp;", "<wbr>&amp;").replace("-", "&#8209;") + "</a>");
		}
		else if (uriInfo.getQueryParameters().containsKey("_summary"))
		{
			String summaryValue = HtmlUtils.htmlEscape(uriInfo.getQueryParameters().getFirst("_summary"));
			u += "?_summary=" + summaryValue;
			heading.append("<a href=\"" + u + "\" title=\"Open " + u + "\">?_summary=" + summaryValue + "</a>");
		}

		return heading.toString();
	}

	private URI getResourceUri(Resource resource, UriInfo uriInfo)
	{
		return getResourceUrlString(resource, uriInfo).map(this::toURI)
				.orElse(toURI(serverBaseUrl + "/" + uriInfo.getPath()));
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

	private Optional<String> getResourceUrlString(Resource resource, UriInfo uriInfo)
	{
		if (resource.getIdElement().hasIdPart())
		{
			if (!uriInfo.getPath().contains("_history"))
				return Optional.of(String.format("%s/%s/%s", serverBaseUrl, resource.getIdElement().getResourceType(),
						resource.getIdElement().getIdPart()));
			else
				return Optional.of(
						String.format("%s/%s/%s/_history/%s", serverBaseUrl, resource.getIdElement().getResourceType(),
								resource.getIdElement().getIdPart(), resource.getIdElement().getVersionIdPart()));
		}
		else if (resource instanceof Bundle b && !resource.getIdElement().hasIdPart())
			return b.getLink().stream().filter(c -> "self".equals(c.getRelation())).findFirst()
					.map(BundleLinkComponent::getUrl);
		else
			return Optional.empty();
	}

	private String toXml(MediaType mediaType, Resource resource) throws IOException
	{
		IParser parser = getParser(mediaType, fhirContext::newXmlParser);

		String content = parser.encodeResourceToString(resource);

		content = content.replace("&amp;", "&amp;amp;").replace("&apos;", "&amp;apos;").replace("&gt;", "&amp;gt;")
				.replace("&lt;", "&amp;lt;").replace("&quot;", "&amp;quot;");
		content = simplifyXml(content);
		content = content.replace("<", "&lt;").replace(">", "&gt;");

		Matcher versionMatcher = XML_ID_UUID_AND_VERSION_PATTERN.matcher(content);
		content = versionMatcher.replaceAll(result ->
		{
			Optional<String> resourceName = getResourceName(resource, result.group(1));
			return resourceName
					.map(rN -> "&lt;id value=\"<a href=\"" + rN + "/" + result.group(1) + "?_format=html\">"
							+ result.group(1) + "</a>\"/&gt;\n" + result.group(2) + "&lt;meta&gt;\n" + result.group(3)
							+ "&lt;versionId value=\"" + "<a href=\"" + rN + "/" + result.group(1) + "/_history/"
							+ result.group(4) + "?_format=html\">" + result.group(4) + "</a>" + "\"/&gt;")
					.orElse(result.group(0));
		});

		Matcher urlMatcher = URL_PATTERN.matcher(content);
		content = urlMatcher.replaceAll(result -> "<a href=\""
				+ result.group().replace("&amp;amp;", "&amp;").replace("&amp;apos;", "&apos;")
						.replace("&amp;gt;", "&gt;").replace("&amp;lt;", "&lt;").replace("&amp;quot;", "&quot;")
				+ "?_format=html\">" + result.group() + "</a>");

		Matcher referenceUuidMatcher = XML_REFERENCE_UUID_PATTERN.matcher(content);
		content = referenceUuidMatcher.replaceAll(result -> "&lt;reference value=\"<a href=\"" + result.group(1)
				+ "?_format=html\">" + result.group(1) + "</a>\"&gt");

		return content;
	}

	private IParser getParser(MediaType mediaType, Supplier<IParser> parserFactor)
	{
		/* Parsers are not guaranteed to be thread safe */
		IParser p = parserFactor.get();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);

		if (mediaType != null)
		{
			switch (mediaType.getParameters().getOrDefault("summary", "false"))
			{
				case "true" -> p.setSummaryMode(true);
				case "text" -> p.setEncodeElements(Set.of("*.text", "*.id", "*.meta", "*.(mandatory)"));
				case "data" -> p.setSuppressNarratives(true);
			}
		}

		p.setPrettyPrint(true);
		return p;
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
		else
			return Optional.of(resource.getClass().getAnnotation(ResourceDef.class).name());
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

	private String toJson(MediaType mediaType, Resource resource) throws IOException
	{
		IParser parser = getParser(mediaType, fhirContext::newJsonParser);

		String content = parser.encodeResourceToString(resource).replace("<", "&lt;").replace(">", "&gt;");

		Matcher urlMatcher = URL_PATTERN.matcher(content);
		content = urlMatcher
				.replaceAll(result -> "<a href=\"" + result.group() + "?_format=html\">" + result.group() + "</a>");

		Matcher referenceUuidMatcher = JSON_REFERENCE_UUID_PATTERN.matcher(content);
		content = referenceUuidMatcher.replaceAll(result -> "\"reference\": \"<a href=\"" + result.group(1)
				+ "?_format=html\">" + result.group(1) + "</a>\",");

		Matcher idUuidMatcher = JSON_ID_UUID_AND_VERSION_PATTERN.matcher(content);
		content = idUuidMatcher.replaceAll(result ->
		{
			Optional<String> resourceName = getResourceName(resource, result.group(1));
			return resourceName
					.map(rN -> "\"id\": \"<a href=\"" + rN + "/" + result.group(1) + "?_format=html\">"
							+ result.group(1) + "</a>\",\n" + result.group(2) + "\"meta\": {\n" + result.group(3)
							+ "\"versionId\": \"" + "<a href=\"" + rN + "/" + result.group(1) + "/_history/"
							+ result.group(4) + "?_format=html\">" + result.group(4) + "</a>" + "\",")
					.orElse(result.group(0));
		});

		return content;
	}
}
