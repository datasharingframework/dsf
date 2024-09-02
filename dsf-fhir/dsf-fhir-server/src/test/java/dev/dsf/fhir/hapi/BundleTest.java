package dev.dsf.fhir.hapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class BundleTest
{
	private static final Logger logger = LoggerFactory.getLogger(BundleTest.class);

	private static FhirContext fhirContext = FhirContext.forR4();

	private IParser newXmlParser()
	{
		IParser p = fhirContext.newXmlParser();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);
		return p;
	}

	private IParser newJsonParser()
	{
		IParser p = fhirContext.newJsonParser();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);
		return p;
	}

	@Test
	public void testBundleXml() throws Exception
	{
		IParser parser = newXmlParser();

		testBundleWithParser(parser);
	}

	@Test
	public void testBundleJson() throws Exception
	{
		IParser parser = newJsonParser();

		testBundleWithParser(parser);
	}

	private void testBundleWithParser(IParser parser)
	{
		Bundle bundle1 = new Bundle();
		bundle1.setType(BundleType.TRANSACTION);

		String orgTempId = "urn:uuid:" + UUID.randomUUID().toString();
		String eptTempId = "urn:uuid:" + UUID.randomUUID().toString();

		Organization org = new Organization();
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");

		Endpoint ept = new Endpoint();
		ept.addIdentifier().setSystem("http://dsf.dev/sid/endpoint-identifier").setValue("Test_Endpoint");

		org.getEndpointFirstRep().setType("Endpoint").setReference(eptTempId);
		ept.getManagingOrganization().setType("Organization").setReference(orgTempId);

		BundleEntryComponent orgEntry = bundle1.addEntry();
		orgEntry.setFullUrl(orgTempId);
		orgEntry.setResource(org);
		orgEntry.getRequest().setMethod(HTTPVerb.PUT)
				.setUrl("Organization?identifier=http://dsf.dev/sid/organization-identifier|Test_Organization");

		BundleEntryComponent eptEntry = bundle1.addEntry();
		eptEntry.setFullUrl(eptTempId);
		eptEntry.setResource(ept);
		eptEntry.getRequest().setMethod(HTTPVerb.PUT)
				.setUrl("Endpoint?identifier=http://dsf.dev/sid/endpoint-identifier|Test_Endpoint");

		String bundle1String = parser.encodeResourceToString(bundle1);
		logger.debug("Bundle1: {}", bundle1String);

		Bundle bundle2 = parser.parseResource(Bundle.class, bundle1String);

		assertTrue(bundle2.getEntry().get(0).getResource() instanceof Organization);
		assertNotNull(((Organization) bundle2.getEntry().get(0).getResource()).getEndpointFirstRep().getResource());

		assertTrue(bundle2.getEntry().get(1).getResource() instanceof Endpoint);
		assertNotNull(((Endpoint) bundle2.getEntry().get(1).getResource()).getManagingOrganization().getResource());

		String bundle2String = parser.encodeResourceToString(bundle2);
		logger.debug("Bundle2: {}", bundle2String);

		assertEquals(bundle1String, bundle2String);
	}

	@Test
	public void testBundleVersionTag() throws Exception
	{
		IdType i = new IdType(null, "id", "version");
		logger.debug(i.withResourceType("Bundle").getValueAsString());

		Bundle b = new Bundle();
		b.setIdElement(new IdType("Bundle", UUID.randomUUID().toString(), "123"));

		String bundleTxt = newXmlParser().encodeResourceToString(b);
		logger.debug(bundleTxt);

		Bundle bRead = newXmlParser().parseResource(Bundle.class, bundleTxt);
		assertEquals("123", bRead.getMeta().getVersionId());
		assertEquals("123", bRead.getIdElement().getVersionIdPart());
	}

	@Test
	public void testParseBundleCheckNoContainedResources() throws Exception
	{
		try (InputStream in = Files.newInputStream(Paths.get("src/test/resources/bundle.xml")))
		{
			Bundle bundle = newXmlParser().parseResource(Bundle.class, in);
			assertNotNull(bundle);
			assertNotNull(bundle.getEntry());
			assertEquals(8, bundle.getEntry().size());

			assertNotNull(bundle.getEntry().get(0));
			assertNotNull(bundle.getEntry().get(0).getResource());
			assertTrue(bundle.getEntry().get(0).getResource() instanceof Organization);

			Organization org = (Organization) bundle.getEntry().get(0).getResource();
			assertNotNull(org.getEndpoint());
			assertEquals(1, org.getEndpoint().size());

			Reference eRef = org.getEndpoint().get(0);
			assertNotNull(eRef);

			// FIXME HAPI FHIR parser adds contained resources to bundle references, getResource() should return null
			assertNotNull(
					"HAPI FHIR parser does not add contained resources to bunlde references anymore, remove workaounds using ReferenceCleaner",
					eRef.getResource());
		}
	}
}
