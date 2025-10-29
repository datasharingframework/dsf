package dev.dsf.fhir.profiles;

import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.ElementDefinition;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.StructureDefinition.StructureDefinitionKind;
import org.hl7.fhir.r4.model.StructureDefinition.TypeDerivationRule;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class StructureDefinitionProfileTest extends AbstractMetaDataResourceProfileTest<StructureDefinition>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-extension-process-authorization-2.0.0.xml",
					"dsf-extension-process-authorization-practitioner-2.0.0.xml",
					"dsf-extension-process-authorization-organization-2.0.0.xml",
					"dsf-extension-process-authorization-organization-practitioner-2.0.0.xml",
					"dsf-extension-process-authorization-parent-organization-role-2.0.0.xml",
					"dsf-extension-process-authorization-parent-organization-role-practitioner-2.0.0.xml",
					"dsf-coding-process-authorization-local-all-2.0.0.xml",
					"dsf-coding-process-authorization-local-all-practitioner-2.0.0.xml",
					"dsf-coding-process-authorization-local-organization-2.0.0.xml",
					"dsf-coding-process-authorization-local-organization-practitioner-2.0.0.xml",
					"dsf-coding-process-authorization-local-parent-organization-role-2.0.0.xml",
					"dsf-coding-process-authorization-local-parent-organization-role-practitioner-2.0.0.xml",
					"dsf-coding-process-authorization-remote-all-2.0.0.xml",
					"dsf-coding-process-authorization-remote-organization-2.0.0.xml",
					"dsf-coding-process-authorization-remote-parent-organization-role-2.0.0.xml",
					"dsf-structure-definition-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml",
					"dsf-practitioner-role-2.0.0.xml", "dsf-process-authorization-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml",
					"dsf-practitioner-role-2.0.0.xml", "dsf-process-authorization-recipient-2.0.0.xml",
					"dsf-process-authorization-requester-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected StructureDefinition create()
	{
		StructureDefinition d = new StructureDefinition();
		d.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/structure-definition");
		d.setUrl("http://test.com/fhir/test");
		d.setVersion("2.0.0");
		d.setName("Test");
		d.setDate(new Date());
		d.setStatus(PublicationStatus.ACTIVE);
		d.setBaseDefinition("http://hl7.org/fhir/StructureDefinition/Patient");
		d.setDerivation(TypeDerivationRule.CONSTRAINT);
		d.setKind(StructureDefinitionKind.RESOURCE);
		d.setAbstract(false);
		d.setType("Patient");

		ElementDefinition e = d.getDifferential().addElement();
		e.setId("Patient.active");
		e.setPath("Patient.active");
		e.setMin(1);

		return d;
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
	public void testActivityDefinition() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-activity-definition-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testBinary() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-binary-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testBundle() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-bundle-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodeSystem() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-code-system-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationLocalAll() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-local-all-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationLocalAllPractitioner() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-local-all-practitioner-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationLocalOrganization() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-local-organization-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationLocalOrganizationPractitioner() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-local-organization-practitioner-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationLocalParentOrganizationRole() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-local-parent-organization-role-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationLocalParentOrganizationRolePractitioner() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-local-parent-organization-role-practitioner-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationRemoteAll() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-remote-all-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationRemoteOrganization() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-remote-organization-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testCodingProcessAuthorizationRemoteParentOrganizationRole() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-coding-process-authorization-remote-parent-organization-role-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testGroup() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-group-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testHealthcareService() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-healthcare-service-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testLibrary() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-library-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testLocation() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-location-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testMeasure() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-measure-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testMeasureReport() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-measure-report-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testMeta() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-meta-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testNamingSystem() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-naming-system-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testOrganization() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-organization-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testOrganizationAffiliation() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-organization-affiliation-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testOrganizationParent() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-organization-parent-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testPatient() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-patient-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testPractitioner() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-practitioner-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testPractitionerRole() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-practitioner-role-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testProvenance() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-provenance-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testQuestionnaire() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-questionnaire-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testQuestionnaireResponse() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class,
				"dsf-questionnaire-response-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testResearchStudy() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-research-study-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testStructureDefinition() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-structure-definition-2.0.0.xml");
		d.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/structure-definition");

		testValid(resourceValidator, d);
	}

	@Test
	public void testSubscription() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-subscription-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testTask() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-task-2.0.0.xml");

		testValid(resourceValidator, d);
	}

	@Test
	public void testValueSet() throws Exception
	{
		StructureDefinition d = readValidationResource(StructureDefinition.class, "dsf-value-set-2.0.0.xml");

		testValid(resourceValidator, d);
	}
}
