package dev.dsf.fhir.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.ValidationResult;

public class ValidatorTest
{
	private static final Logger logger = LoggerFactory.getLogger(ValidatorTest.class);

	private static final FhirContext fhirContext = FhirContext.forR4();

	@Test
	public void validateBundleInBundle() throws Exception
	{
		ResourceValidator validator = createValidator();

		Bundle b = createBundleInBundle();

		ValidationResult result = validator.validate(b);

		logger.debug("validation messages: {}", result.getMessages());

		assertNotNull(result);
		assertNotNull(result.getMessages());
		assertEquals("If this assert fails, maybe remove ResourceInResourceValidator workaround", 2,
				result.getMessages().size());
	}

	// XXX Tests workaround for HAPI bug, unable to validate Bundles containing Bundles
	// Bug may be related to https://github.com/hapifhir/org.hl7.fhir.core/issues/1889
	@Test
	public void validateBundleInBundleWorkaround() throws Exception
	{
		ResourceValidator validator = new ResourceInResourceValidator(fhirContext, createValidator());

		Bundle b = createBundleInBundle();

		ValidationResult result = validator.validate(b);

		logger.debug("validation messages: {}", result.getMessages());

		assertNotNull(result);
		assertNotNull(result.getMessages());
		assertTrue(result.getMessages().isEmpty());
	}

	private Bundle createBundleInBundle()
	{
		Bundle b = new Bundle().setType(BundleType.BATCHRESPONSE);
		b.addEntry()
				.setResource(new Bundle().setType(BundleType.SEARCHSET)
						.addLink(new BundleLinkComponent().setRelation("self").setUrl("Medication")).setTotal(0))
				.setResponse(new BundleEntryResponseComponent().setStatus("200"));
		return b;
	}

	private ResourceValidator createValidator()
	{
		DefaultProfileValidationSupport dpvs = new DefaultProfileValidationSupport(fhirContext);
		dpvs.fetchAllStructureDefinitions();

		IValidationSupport chain = new SimpleValidationSupportChain(fhirContext,
				new InMemoryTerminologyServerValidationSupport(fhirContext), dpvs,
				new CommonCodeSystemsTerminologyService(fhirContext));

		IValidationSupport cache = new ValidationSupportWithCache(fhirContext, chain);
		return new ResourceValidatorImpl(fhirContext, cache);
	}
}
