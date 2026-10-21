package dev.dsf.fhir.adapter;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ca.uhn.fhir.context.FhirContext;

import dev.dsf.common.ui.theme.Theme;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

@RunWith(MockitoJUnitRunner.class)
public class ThymeleafTemplateServiceTest
{
	private static final String BASE_URL = "http://test.local";
	private static final MediaType MEDIA_TYPE = MediaType.TEXT_HTML_TYPE;
	private static final String SELF_CLOSING_TAG_REGEX = "&lt;reference value=\"<a href=\"\\w+/.*_format=html\">\\w+/.*</a>\"/&gt";
	private static final Pattern SELF_CLOSING_TAG_PATTERN = Pattern.compile(SELF_CLOSING_TAG_REGEX);
	private static final String NO_SELF_CLOSING_TAG_REGEX = "\"&lt;reference value=\"<a href=\"\\w+/.*_format=html\">\\w+/.*</a>\"&gt\";";
	public static final Pattern NO_SELF_CLOSING_TAG_PATTERN = Pattern.compile(NO_SELF_CLOSING_TAG_REGEX);

	private final FhirContext fhirContext = FhirContext.forR4();

	@Mock
	private Theme theme;

	@Mock
	private List<? extends ThymeleafContext> contexts;

	@Mock
	private UriInfo uriInfo;

	@Mock
	private SecurityContext securityContext;

	@Test
	public void testSelfClosingReferenceTags() throws Exception
	{
		Mockito.when(uriInfo.getPath()).thenReturn("");
		Mockito.when(uriInfo.getQueryParameters()).thenReturn(new MultivaluedHashMap<>());

		ThymeleafTemplateService thymeleafTemplateService = new ThymeleafTemplateServiceImpl(BASE_URL, theme, fhirContext, contexts, false, false);

		ByteArrayOutputStream xmlOutputStream = new ByteArrayOutputStream();
		thymeleafTemplateService.writeTo(getExampleOrganization(), Organization.class, MEDIA_TYPE, uriInfo, securityContext, xmlOutputStream);
		String xmlContent = toString(xmlOutputStream.toByteArray());

		assertEquals(1, getNumSelfClosingReferencesWithSlash(xmlContent));
		assertEquals(0, getNumSelfClosingReferencesMissingSlash(xmlContent));
		xmlOutputStream.close();

		xmlOutputStream = new ByteArrayOutputStream();
		thymeleafTemplateService.writeTo(getExampleOrganizationAffiliation(), OrganizationAffiliation.class, MEDIA_TYPE, uriInfo, securityContext, xmlOutputStream);
		xmlContent = toString(xmlOutputStream.toByteArray());

		assertEquals(3, getNumSelfClosingReferencesWithSlash(xmlContent));
		assertEquals(0, getNumSelfClosingReferencesMissingSlash(xmlContent));
	}

	private Organization getExampleOrganization()
	{
		Organization organization = new Organization();
		organization.setId(UUID.randomUUID().toString());
		organization.addEndpoint()
				.setReference("Endpoint/" + UUID.randomUUID());
		return organization;
	}

	private OrganizationAffiliation getExampleOrganizationAffiliation()
	{
		OrganizationAffiliation organizationAffiliation = new OrganizationAffiliation();
		organizationAffiliation.setId(UUID.randomUUID().toString());
		organizationAffiliation.setOrganization(new Reference("Organization/" +  UUID.randomUUID()));
		organizationAffiliation.setParticipatingOrganization(new Reference("Organization/" +  UUID.randomUUID()));
		organizationAffiliation.addEndpoint()
				.setReference("Endpoint/" +  UUID.randomUUID());

		return organizationAffiliation;
	}

	private String toString(byte[] content)
	{
		return new String(content, StandardCharsets.UTF_8);
	}

	private int getNumSelfClosingReferencesWithSlash(String content)
	{
		int matches = 0;
		Matcher matcher = SELF_CLOSING_TAG_PATTERN.matcher(content);
		while(matcher.find())
		{
			matches++;
		}
		return matches;
	}

	private int getNumSelfClosingReferencesMissingSlash(String content)
	{
		int matches = 0;
		Matcher matcher = NO_SELF_CLOSING_TAG_PATTERN.matcher(content);
		while(matcher.find())
		{
			matches++;
		}
		return matches;
	}
}
