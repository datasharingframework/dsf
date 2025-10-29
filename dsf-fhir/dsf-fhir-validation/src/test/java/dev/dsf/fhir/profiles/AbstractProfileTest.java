package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class AbstractProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractProfileTest.class);

	protected static final FhirContext context = FhirContext.forR4();

	protected final void logResource(Resource resource)
	{
		logger.trace("{}",
				context.newJsonParser().setStripVersionsFromReferences(false)
						.setOverrideResourceIdWithBundleEntryFullUrl(false).setPrettyPrint(false)
						.encodeResourceToString(resource));
	}

	protected final void testValid(ResourceValidator validator, Resource resource)
	{
		logResource(resource);

		ValidationResult result = validator.validate(resource);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	protected final void testNotValid(ResourceValidator validator, Resource resource, int errorCount)
	{
		logResource(resource);

		ValidationResult result = validator.validate(resource);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertFalse(result.isSuccessful());
		assertEquals(errorCount,
				result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())).count());
	}

	protected <R extends Resource> R readValidationResource(Class<R> resourceType, String filename) throws IOException
	{
		try (InputStream in = Files.newInputStream(
				Paths.get("src/main/resources/fhir", resourceType.getAnnotation(ResourceDef.class).name(), filename)))
		{
			return context.newXmlParser().parseResource(resourceType, in);
		}
	}
}
