package dev.dsf.fhir.profiles;

import java.util.List;

import org.hl7.fhir.r4.model.Patient;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class PatientProfileTest extends AbstractMetaTagProfileTest<Patient>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-patient-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected Patient create()
	{
		Patient p = new Patient();
		p.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/patient");

		return p;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}
}
