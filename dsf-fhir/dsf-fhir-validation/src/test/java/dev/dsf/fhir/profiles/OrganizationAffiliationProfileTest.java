package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.UUID;

import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class OrganizationAffiliationProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationAffiliationProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			Arrays.asList("dsf-organization-affiliation-1.0.0.xml", "dsf-organization-1.0.0.xml",
					"dsf-organization-parent-1.0.0.xml"),
			Arrays.asList("dsf-read-access-tag-1.0.0.xml", "dsf-organization-role-1.0.0.xml"),
			Arrays.asList("dsf-read-access-tag-1.0.0.xml", "dsf-organization-role-1.0.0.xml"));

	private ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testOrganizationAffiliationProfileValid() throws Exception
	{
		OrganizationAffiliation a = new OrganizationAffiliation();
		a.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization-affiliation");
		a.setActive(true);
		a.getOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		a.getParticipatingOrganization().setReference("Organization/" + UUID.randomUUID().toString());
		a.getCodeFirstRep().getCodingFirstRep().setSystem("http://dsf.dev/fhir/CodeSystem/organization-role")
				.setCode("DIC");
		a.getEndpointFirstRep().setReference("Endpoint/" + UUID.randomUUID().toString());

		ValidationResult result = resourceValidator.validate(a);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}
}
