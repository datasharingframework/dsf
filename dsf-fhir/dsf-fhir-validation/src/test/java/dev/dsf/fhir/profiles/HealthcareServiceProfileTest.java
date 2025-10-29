package dev.dsf.fhir.profiles;

import java.util.List;

import org.hl7.fhir.r4.model.HealthcareService;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class HealthcareServiceProfileTest extends AbstractMetaTagProfileTest<HealthcareService>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-healthcare-service-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected HealthcareService create()
	{
		HealthcareService s = new HealthcareService();
		s.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/healthcare-service");

		return s;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}
}
