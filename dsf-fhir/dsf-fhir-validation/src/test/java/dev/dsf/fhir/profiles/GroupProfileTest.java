package dev.dsf.fhir.profiles;

import java.util.List;

import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupType;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class GroupProfileTest extends AbstractMetaTagProfileTest<Group>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-group-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected Group create()
	{
		Group g = new Group();
		g.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/group");
		g.setType(GroupType.PERSON);
		g.setActual(false);

		return g;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}
}
