package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.ActivityDefinition.ActivityDefinitionKind;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.StringType;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class ActivityDefinitionProfileTest extends AbstractProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(ActivityDefinitionProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(context,
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
					"dsf-activity-definition-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml",
					"dsf-practitioner-role-2.0.0.xml", "dsf-process-authorization-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml",
					"dsf-practitioner-role-2.0.0.xml", "dsf-process-authorization-recipient-2.0.0.xml",
					"dsf-process-authorization-requester-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	private ActivityDefinition createActivityDefinition()
	{
		var ad = new ActivityDefinition();
		ad.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/activity-definition");
		ad.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		ad.setUrl("http://dsf.dev/bpe/Process/test");
		ad.setVersion("2.0.0");
		ad.setStatus(PublicationStatus.ACTIVE);
		ad.setKind(ActivityDefinitionKind.TASK);
		ad.setName("TestProcess");

		return ad;
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterRemoteAllRecipientLocalAllValid()
			throws Exception
	{
		ActivityDefinition ad = createActivityDefinition();
		Extension processAuthorization = ad.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension("message-name", new StringType("foo"));
		processAuthorization.addExtension("task-profile",
				new CanonicalType("http://bar.org/fhir/StructureDefinition/baz"));
		processAuthorization.addExtension("requester",
				new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "REMOTE_ALL", null));
		processAuthorization.addExtension("recipient",
				new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "LOCAL_ALL", null));

		logResource(ad);

		ValidationResult result = resourceValidator.validate(ad);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterLocalPractitionerRoleRecipientLocalAllValid()
			throws Exception
	{
		ActivityDefinition ad = createActivityDefinition();
		Extension processAuthorization = ad.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension("message-name", new StringType("foo"));
		processAuthorization.addExtension("task-profile",
				new CanonicalType("http://bar.org/fhir/StructureDefinition/baz"));

		Coding requesterCoding = new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization",
				"LOCAL_ALL_PRACTITIONER", null);
		requesterCoding.addExtension(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-practitioner",
				new Coding("http://dsf.dev/fhir/CodeSystem/user-role", "DIC_USER", null));
		processAuthorization.addExtension("requester", requesterCoding);
		processAuthorization.addExtension("recipient",
				new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "LOCAL_ALL", null));

		logResource(ad);

		ValidationResult result = resourceValidator.validate(ad);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterRemoteOrganizationRecipientLocalParentOrganizationRoleValid()
			throws Exception
	{
		ActivityDefinition ad = createActivityDefinition();
		Extension processAuthorization = ad.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension("message-name", new StringType("foo"));
		processAuthorization.addExtension("task-profile",
				new CanonicalType("http://bar.org/fhir/StructureDefinition/baz"));

		Coding requesterCoding = new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization",
				"REMOTE_ORGANIZATION", null);
		requesterCoding.addExtension(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization",
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("organization.com"));
		processAuthorization.addExtension("requester", requesterCoding);

		Coding recipientCoding = new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "LOCAL_ROLE", null);
		Extension recipientCodingExtension = recipientCoding.addExtension();
		recipientCodingExtension.setUrl(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role");
		recipientCodingExtension.addExtension("parent-organization",
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org"));
		recipientCodingExtension.addExtension("organization-role",
				new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DIC", null));
		processAuthorization.addExtension("recipient", recipientCoding);

		logResource(ad);

		ValidationResult result = resourceValidator.validate(ad);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterOrganizationPractitionerRoleRecipientLocalParentOrganizationRoleValid()
			throws Exception
	{
		ActivityDefinition ad = createActivityDefinition();
		Extension processAuthorization = ad.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension("message-name", new StringType("foo"));
		processAuthorization.addExtension("task-profile",
				new CanonicalType("http://bar.org/fhir/StructureDefinition/baz"));

		Coding requesterCoding = new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization",
				"LOCAL_ORGANIZATION_PRACTITIONER", null);
		Extension requesterCodingExtension = requesterCoding.addExtension();
		requesterCodingExtension.setUrl(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization-practitioner");
		requesterCodingExtension.addExtension("organization",
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("organization.com"));
		requesterCodingExtension.addExtension("practitioner-role",
				new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DSF_ADMIN", null));
		processAuthorization.addExtension("requester", requesterCoding);

		Coding recipientCoding = new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "LOCAL_ROLE", null);
		Extension recipientCodingExtension = recipientCoding.addExtension();
		recipientCodingExtension.setUrl(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role");
		recipientCodingExtension.addExtension("parent-organization",
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org"));
		recipientCodingExtension.addExtension("organization-role",
				new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DIC", null));
		processAuthorization.addExtension("recipient", recipientCoding);

		logResource(ad);

		ValidationResult result = resourceValidator.validate(ad);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterParentOrganizationRolePractitionerRoleRecipientLocalAllValid()
			throws Exception
	{
		ActivityDefinition ad = createActivityDefinition();
		Extension processAuthorization = ad.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension("message-name", new StringType("foo"));
		processAuthorization.addExtension("task-profile",
				new CanonicalType("http://bar.org/fhir/StructureDefinition/baz"));

		Coding requesterCoding = new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization",
				"LOCAL_ROLE_PRACTITIONER", null);
		Extension requesterCodingExtension = requesterCoding.addExtension();
		requesterCodingExtension.setUrl(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role-practitioner");
		requesterCodingExtension.addExtension("parent-organization",
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org"));
		requesterCodingExtension.addExtension("organization-role",
				new Coding("http://dsf.dev/fhir/CodeSystem/organization-role", "DIC", null));
		requesterCodingExtension.addExtension("practitioner-role",
				new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DSF_ADMIN", null));
		processAuthorization.addExtension("requester", requesterCoding);

		processAuthorization.addExtension("recipient",
				new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "LOCAL_ALL", null));

		logResource(ad);

		ValidationResult result = resourceValidator.validate(ad);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterRemoteOrganizationRecipientRemoteConsortiumRoleNotValid()
			throws Exception
	{
		ActivityDefinition ad = createActivityDefinition();
		Extension processAuthorization = ad.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension("message-name", new StringType("foo"));
		processAuthorization.addExtension("task-profile",
				new CanonicalType("http://bar.org/fhir/StructureDefinition/baz"));

		Coding requesterCoding = new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization",
				"REMOTE_ORGANIZATION", null);
		requesterCoding.addExtension(
				"http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization",
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("organization.com"));
		processAuthorization.addExtension("requester", requesterCoding);

		Coding recipientCoding = new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "REMOTE_ROLE",
				null);
		processAuthorization.addExtension("recipient", recipientCoding);

		logResource(ad);

		ValidationResult result = resourceValidator.validate(ad);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertFalse(result.isSuccessful());
		assertEquals(9, result.getMessages().size());
	}

	@Test
	public void testActivityDefinitionWithoutProcessAuthorizationNotValid() throws Exception
	{
		ActivityDefinition ad = createActivityDefinition();

		logResource(ad);

		ValidationResult result = resourceValidator.validate(ad);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertFalse(result.isSuccessful());
		assertEquals(2, result.getMessages().size());
	}
}
