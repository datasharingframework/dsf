package dev.dsf.fhir.profiles;

import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.NamingSystem.NamingSystemIdentifierType;
import org.hl7.fhir.r4.model.NamingSystem.NamingSystemType;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class NamingSystemProfileTest extends AbstractMetaTagProfileTest<NamingSystem>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-extension-check-logical-reference-2.0.0.xml", "dsf-naming-system-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	protected NamingSystem create()
	{
		NamingSystem s = new NamingSystem();
		s.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/naming-system");
		s.setName("Test");
		s.setDate(new Date());
		s.setStatus(PublicationStatus.ACTIVE);
		s.setKind(NamingSystemType.IDENTIFIER);
		s.getUniqueIdFirstRep().setType(NamingSystemIdentifierType.OTHER)
				.setValue("http://dsf.dev/sid/test-identifier");

		return s;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}

	@Test
	public void testNotValidNoName() throws Exception
	{
		NamingSystem s = create();
		s.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		s.setName(null);

		testNotValid(resourceValidator, s, 1);
	}

	@Test
	public void testNotValidNoDate() throws Exception
	{
		NamingSystem s = create();
		s.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		s.setDate(null);

		testNotValid(resourceValidator, s, 1);
	}

	@Test
	public void testDsfEndpoint() throws Exception
	{
		NamingSystem s = readValidationResource(NamingSystem.class, "dsf-endpoint.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testDsfOrganization() throws Exception
	{
		NamingSystem s = readValidationResource(NamingSystem.class, "dsf-organization.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testDsfPractitioner() throws Exception
	{
		NamingSystem s = readValidationResource(NamingSystem.class, "dsf-practitioner.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testDsfTask() throws Exception
	{
		NamingSystem s = readValidationResource(NamingSystem.class, "dsf-task.xml");

		testValid(resourceValidator, s);
	}
}
