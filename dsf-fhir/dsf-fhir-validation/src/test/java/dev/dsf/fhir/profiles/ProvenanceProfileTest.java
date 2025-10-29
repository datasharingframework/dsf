package dev.dsf.fhir.profiles;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Provenance;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class ProvenanceProfileTest extends AbstractMetaTagProfileTest<Provenance>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-provenance-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	protected Provenance create()
	{
		Provenance p = new Provenance();
		p.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/provenance");
		p.getTargetFirstRep().setReference("http://localhost/fhir/Binary/" + UUID.randomUUID().toString());
		p.setRecorded(new Date());
		p.getAgentFirstRep().getWho()
				.setReference("http://localhost/fhir/Practitioner/" + UUID.randomUUID().toString());

		return p;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}
}
