package dev.dsf.bpe.test.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.service.MimetypeService;
import dev.dsf.bpe.v2.variables.Variables;

public class MimetypeServiceTest extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, api.getMimetypeService());
	}

	@PluginTest
	public void testAttachmentBundle(MimetypeService mimetypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"/fhir/Bundle/DocumentReference-with-Attachment-Bundle.xml");
		testResourcesStream(resources, mimetypeService);
	}

	@PluginTest
	public void testAttachmentCsv(MimetypeService mimetypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"/fhir/Bundle/DocumentReference-with-Attachment-CSV.xml");
		testResourcesStream(resources, mimetypeService);
	}

	@PluginTest
	public void testAttachmentMeasureReport(MimetypeService mimetypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"/fhir/Bundle/DocumentReference-with-Attachment-MeasureReport.xml");
		testResourcesStream(resources, mimetypeService);
	}

	@PluginTest
	public void testAttachmentNdJson(MimetypeService mimetypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"/fhir/Bundle/DocumentReference-with-Attachment-NdJson.xml");
		testResourcesStream(resources, mimetypeService);
	}

	@PluginTest
	public void testAttachmentZip(MimetypeService mimetypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"/fhir/Bundle/DocumentReference-with-Attachment-ZIP.xml");
		testResourcesStream(resources, mimetypeService);
	}

	private List<Resource> getResourcesNotDocumentReferenceFromPath(String pathToBundle)
	{
		try (InputStream input = getClass().getResourceAsStream(pathToBundle))
		{
			Bundle bundle = FhirContext.forR4().newXmlParser().parseResource(Bundle.class, input);
			return bundle.getEntry().stream().filter(Bundle.BundleEntryComponent::hasResource)
					.map(Bundle.BundleEntryComponent::getResource).filter(r -> !(r instanceof DocumentReference))
					.toList();
		}
		catch (IOException exception)
		{
			throw new RuntimeException(exception);
		}
	}

	private void testResourcesStream(List<Resource> resources, MimetypeService mimetypeService)
	{
		for (Resource resource : resources)
		{
			InputStream data = getDataStream(resource);
			String expected = getMimetype(resource);

			mimetypeService.validate(data, expected);
		}
	}

	private InputStream getDataStream(Resource resource)
	{
		if (resource instanceof Binary binary)
			return new ByteArrayInputStream(binary.getData());
		else
			return new ByteArrayInputStream(FhirContext.forR4().newXmlParser().encodeResourceToString(resource)
					.getBytes(StandardCharsets.UTF_8));
	}

	private String getMimetype(Resource resource)
	{
		if (resource instanceof Binary binary)
			return binary.getContentType();
		else
			return "application/fhir+xml";
	}
}
