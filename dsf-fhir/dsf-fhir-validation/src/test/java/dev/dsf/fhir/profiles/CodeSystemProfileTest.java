package dev.dsf.fhir.profiles;

import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.CodeSystem.CodeSystemContentMode;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class CodeSystemProfileTest extends AbstractMetaDataResourceProfileTest<CodeSystem>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-code-system-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected CodeSystem create()
	{
		CodeSystem s = new CodeSystem();
		s.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/code-system");
		s.setUrl("http://test.com/fhir/test");
		s.setVersion("2.0.0");
		s.setName("Test");
		s.setDate(new Date());
		s.setStatus(PublicationStatus.ACTIVE);
		s.setContent(CodeSystemContentMode.COMPLETE);
		s.addConcept().setCode("TEST").setDisplay("Test Display").setDefinition("Test Definition");
		s.setCaseSensitive(true);

		return s;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}

	@Test
	public void runMetaDataResourceTests() throws Exception
	{
		doRunMetaDataResourceTests(resourceValidator);
	}

	@Test
	public void testBpmnMessage() throws Exception
	{
		CodeSystem s = readValidationResource(CodeSystem.class, "dsf-bpmn-message-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testOrganizationRole() throws Exception
	{
		CodeSystem s = readValidationResource(CodeSystem.class, "dsf-organization-role-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testPractitionerRole() throws Exception
	{
		CodeSystem s = readValidationResource(CodeSystem.class, "dsf-practitioner-role-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testProcessAuthorization() throws Exception
	{
		CodeSystem s = readValidationResource(CodeSystem.class, "dsf-process-authorization-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testReadAccessTag() throws Exception
	{
		CodeSystem s = readValidationResource(CodeSystem.class, "dsf-read-access-tag-2.0.0.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testRrnIetfBcp13() throws Exception
	{
		CodeSystem s = readValidationResource(CodeSystem.class, "urn_ietf_bcp_13.xml");

		testValid(resourceValidator, s);
	}
}
