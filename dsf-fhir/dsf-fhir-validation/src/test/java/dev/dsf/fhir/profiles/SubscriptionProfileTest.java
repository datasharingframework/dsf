package dev.dsf.fhir.profiles;

import java.util.List;

import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.junit.ClassRule;
import org.junit.Test;

import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class SubscriptionProfileTest extends AbstractMetaTagProfileTest<Subscription>
{
	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-subscription-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	protected Subscription create()
	{
		var s = new Subscription();
		s.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/subscription");
		s.setStatus(SubscriptionStatus.ACTIVE);
		s.setReason("Reasons");
		s.setCriteria("Patient");
		s.getChannel().setType(SubscriptionChannelType.WEBSOCKET);

		return s;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}

	@Test
	public void testValidPayloadFhirJson() throws Exception
	{
		Subscription s = create();
		s.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		s.getChannel().setPayload("application/fhir+json");

		testValid(resourceValidator, s);
	}

	@Test
	public void testValidPayloadFhirXml() throws Exception
	{
		Subscription s = create();
		s.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		s.getChannel().setPayload("application/fhir+xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testNotValidTypeNotWebsocket() throws Exception
	{
		Subscription s = create();
		s.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		s.getChannel().setType(SubscriptionChannelType.EMAIL);

		testNotValid(resourceValidator, s, 1);
	}

	@Test
	public void testNotValidPayloadNotFhirJsonOrFhirXml() throws Exception
	{
		Subscription s = create();
		s.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);
		s.getChannel().setPayload("application/pdf");

		testNotValid(resourceValidator, s, 1);
	}

	@Test
	public void testDsfBpmnQuestionnaireResponseSubscription() throws Exception
	{
		Subscription s = readValidationResource(Subscription.class, "dsf-bpmn-questionnaire-response-subscription.xml");

		testValid(resourceValidator, s);
	}

	@Test
	public void testDsfBpmnTaskSubscription() throws Exception
	{
		Subscription s = readValidationResource(Subscription.class, "dsf-bpmn-task-subscription.xml");

		testValid(resourceValidator, s);
	}
}
