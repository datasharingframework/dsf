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
import dev.dsf.bpe.v2.service.MimeTypeService;
import dev.dsf.bpe.v2.variables.Variables;

public class MimeTypeServiceTest extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, api.getMimeTypeService());
	}

	@PluginTest
	public void testAttachmentBundle(MimeTypeService mimeTypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"fhir/Bundle/DocumentReference-with-Attachment-Bundle.xml");
		testResourcesStream(resources, mimeTypeService);
	}

	@PluginTest
	public void testAttachmentCsv(MimeTypeService mimeTypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"fhir/Bundle/DocumentReference-with-Attachment-CSV.xml");
		testResourcesStream(resources, mimeTypeService);
	}

	@PluginTest
	public void testAttachmentMeasureReport(MimeTypeService mimeTypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"fhir/Bundle/DocumentReference-with-Attachment-MeasureReport.xml");
		testResourcesStream(resources, mimeTypeService);
	}

	@PluginTest
	public void testAttachmentNdJson(MimeTypeService mimeTypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"fhir/Bundle/DocumentReference-with-Attachment-NdJson.xml");
		testResourcesStream(resources, mimeTypeService);
	}

	@PluginTest
	public void testAttachmentZip(MimeTypeService mimeTypeService)
	{
		List<Resource> resources = getResourcesNotDocumentReferenceFromPath(
				"fhir/Bundle/DocumentReference-with-Attachment-ZIP.xml");
		testResourcesStream(resources, mimeTypeService);
	}

	private List<Resource> getResourcesNotDocumentReferenceFromPath(String pathToBundle)
	{
		try (InputStream input = MimeTypeServiceTest.class.getClassLoader().getResourceAsStream(pathToBundle))
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

	private void testResourcesStream(List<Resource> resources, MimeTypeService mimeTypeService)
	{
		for (Resource resource : resources)
		{
			InputStream data = getDataStream(resource);
			String mimeType = getMimetype(resource);

			MimeTypeService.ValidationResult validationResult = mimeTypeService.validateWithResult(data, mimeType);
			if (!validationResult.mimeTypesMatch())
				throw new RuntimeException(
						"Detected MIME type does not match expected MIME type (#validateWithResult())");

			boolean mimeTypesMatch = mimeTypeService.validateWithBoolean(data, mimeType);
			if (!mimeTypesMatch)
				throw new RuntimeException(
						"Detected MIME type does not match expected MIME type (#validateWithBoolean())");

			try
			{
				mimeTypeService.validateWithException(data, mimeType);
			}
			catch (Exception e)
			{
				throw new RuntimeException(
						"Detected MIME type does not match expected MIME type (#validateWithException()) - "
								+ e.getMessage());
			}
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
