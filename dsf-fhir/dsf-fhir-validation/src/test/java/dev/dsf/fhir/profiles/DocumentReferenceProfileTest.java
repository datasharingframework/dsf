package dev.dsf.fhir.profiles;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.DocumentReference;
import org.hl7.fhir.r4.model.Enumerations.DocumentReferenceStatus;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class DocumentReferenceProfileTest extends AbstractMetaTagProfileTest<DocumentReference>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-document-reference-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	protected DocumentReference create()
	{
		DocumentReference r = new DocumentReference();
		r.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/document-reference");
		r.setStatus(DocumentReferenceStatus.CURRENT);
		r.getContentFirstRep().getAttachment().setUrl("https://localhost/fhir/Binary/" + UUID.randomUUID());

		return r;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}
}
