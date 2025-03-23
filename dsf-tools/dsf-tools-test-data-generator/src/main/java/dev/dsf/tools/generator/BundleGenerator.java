package dev.dsf.tools.generator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

public class BundleGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(BundleGenerator.class);

	private static final String BUNDLE_TEMPLATE_FILE = "/bundle-templates/test-bundle.xml";

	private final CertificateGenerator certificateGenerator;

	public BundleGenerator(CertificateGenerator certificateGenerator)
	{
		Objects.requireNonNull(certificateGenerator, "certificateGenerator");

		this.certificateGenerator = certificateGenerator;
	}

	private Bundle readAndCleanBundle()
	{
		try (InputStream in = BundleGenerator.class.getResourceAsStream(BUNDLE_TEMPLATE_FILE))
		{
			IParser parser = FhirContext.forR4().newXmlParser();
			parser.setStripVersionsFromReferences(false);
			parser.setOverrideResourceIdWithBundleEntryFullUrl(false);
			parser.setPrettyPrint(true);

			return parser.parseResource(Bundle.class, in);
		}
		catch (IOException e)
		{
			logger.error("Unable to read bundle from {}", BUNDLE_TEMPLATE_FILE, e);
			throw new RuntimeException(e);
		}
	}

	public Bundle getTestBundle()
	{
		Bundle bundle = readAndCleanBundle();

		Organization organization = (Organization) bundle.getEntry().get(0).getResource();
		Extension thumbprintExtension = organization
				.getExtensionByUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint");
		thumbprintExtension.setValue(new StringType(certificateGenerator.getCertificateThumbprintsByCommonNameAsHex()
				.get(CertificateGenerator.SUBJECT_CN_BPE)));

		return bundle;
	}
}
