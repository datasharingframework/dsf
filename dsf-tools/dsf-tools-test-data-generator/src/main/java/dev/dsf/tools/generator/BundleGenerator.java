package dev.dsf.tools.generator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.tools.generator.CertificateGenerator.CertificateFiles;

public class BundleGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(BundleGenerator.class);

	private final FhirContext fhirContext = FhirContext.forR4();

	private Bundle testBundle;

	private Bundle readAndCleanBundle(Path bundleTemplateFile)
	{
		try (InputStream in = Files.newInputStream(bundleTemplateFile))
		{
			return newXmlParser().parseResource(Bundle.class, in);
		}
		catch (IOException e)
		{
			logger.error("Error while reading bundle from {}", bundleTemplateFile.toString(), e);
			throw new RuntimeException(e);
		}
	}

	private void writeBundle(Path bundleFile, Bundle bundle)
	{
		try (OutputStream out = Files.newOutputStream(bundleFile);
				OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8))
		{
			newXmlParser().encodeResourceToWriter(bundle, writer);
		}
		catch (IOException e)
		{
			logger.error("Error while writing bundle to {}", bundleFile.toString(), e);
			throw new RuntimeException(e);
		}
	}

	private IParser newXmlParser()
	{
		IParser parser = fhirContext.newXmlParser();
		parser.setStripVersionsFromReferences(false);
		parser.setOverrideResourceIdWithBundleEntryFullUrl(false);
		parser.setPrettyPrint(true);
		return parser;
	}

	public void createTestBundle(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		Path testBundleTemplateFile = Paths.get("src/main/resources/bundle-templates/test-bundle.xml");

		testBundle = readAndCleanBundle(testBundleTemplateFile);

		Organization organization = (Organization) testBundle.getEntry().get(0).getResource();
		Extension thumbprintExtension = organization
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint");
		thumbprintExtension.setValue(new StringType(
				clientCertificateFilesByCommonName.get("test-client").getCertificateSha512ThumbprintHex()));

		writeBundle(Paths.get("bundle/test-bundle.xml"), testBundle);
	}

	public void copyJavaTestBundle()
	{
		Path javaTestBundleFile = Paths.get("../../dsf-fhir/dsf-fhir-server-jetty/conf/bundle.xml");
		logger.info("Copying fhir bundle to {}", javaTestBundleFile);
		writeBundle(javaTestBundleFile, testBundle);
	}
}
