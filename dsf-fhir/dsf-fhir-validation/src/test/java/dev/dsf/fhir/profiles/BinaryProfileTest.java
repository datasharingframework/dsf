package dev.dsf.fhir.profiles;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Binary;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class BinaryProfileTest extends AbstractMetaTagProfileTest<Binary>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-binary-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Override
	protected Binary create()
	{
		Binary b = new Binary();
		b.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/binary");
		b.setContent("Test".getBytes(StandardCharsets.UTF_8));
		b.setContentType("Text/Plain");

		return b;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}

	@Test
	public void testBinaryWithSecurityContextValid() throws Exception
	{
		Binary b = create();
		b.getSecurityContext().setReference("DocumentReference/" + UUID.randomUUID().toString());

		testValid(resourceValidator, b);
	}
}
