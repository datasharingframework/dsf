package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
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

public class ActivityDefinitionProfileTest extends AbstractMetaDataResourceProfileTest<ActivityDefinition>
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

	@Override
	protected ActivityDefinition create()
	{
		ActivityDefinition d = createWithoutAuthExtension();

		Extension processAuthorization = d.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension("message-name", new StringType("foo"));
		processAuthorization.addExtension("task-profile",
				new CanonicalType("http://bar.org/fhir/StructureDefinition/baz"));
		processAuthorization.addExtension("requester",
				new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "REMOTE_ALL", null));
		processAuthorization.addExtension("recipient",
				new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "LOCAL_ALL", null));

		return d;
	}

	private ActivityDefinition createWithoutAuthExtension()
	{
		ActivityDefinition d = new ActivityDefinition();
		d.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/activity-definition");
		d.setUrl("http://dsf.dev/bpe/Process/test");
		d.setVersion("2.0.0");
		d.setStatus(PublicationStatus.ACTIVE);
		d.setKind(ActivityDefinitionKind.TASK);
		d.setName("TestProcess");
		d.setDate(new Date());

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
	public void testActivityDefinitionWithProcessAuthorizationRequesterRemoteAllRecipientLocalAllValid()
			throws Exception
	{
		ActivityDefinition d = createWithoutAuthExtension();
		d.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		Extension processAuthorization = d.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-process-authorization");
		processAuthorization.addExtension("message-name", new StringType("foo"));
		processAuthorization.addExtension("task-profile",
				new CanonicalType("http://bar.org/fhir/StructureDefinition/baz"));
		processAuthorization.addExtension("requester",
				new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "REMOTE_ALL", null));
		processAuthorization.addExtension("recipient",
				new Coding("http://dsf.dev/fhir/CodeSystem/process-authorization", "LOCAL_ALL", null));

		logResource(d);

		ValidationResult result = resourceValidator.validate(d);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterLocalPractitionerRoleRecipientLocalAllValid()
			throws Exception
	{
		ActivityDefinition d = createWithoutAuthExtension();
		d.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		Extension processAuthorization = d.addExtension()
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

		logResource(d);

		ValidationResult result = resourceValidator.validate(d);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterRemoteOrganizationRecipientLocalParentOrganizationRoleValid()
			throws Exception
	{
		ActivityDefinition d = createWithoutAuthExtension();
		d.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		Extension processAuthorization = d.addExtension()
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

		logResource(d);

		ValidationResult result = resourceValidator.validate(d);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterOrganizationPractitionerRoleRecipientLocalParentOrganizationRoleValid()
			throws Exception
	{
		ActivityDefinition d = createWithoutAuthExtension();
		d.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		Extension processAuthorization = d.addExtension()
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

		logResource(d);

		ValidationResult result = resourceValidator.validate(d);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterParentOrganizationRolePractitionerRoleRecipientLocalAllValid()
			throws Exception
	{
		ActivityDefinition d = createWithoutAuthExtension();
		d.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		Extension processAuthorization = d.addExtension()
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

		logResource(d);

		ValidationResult result = resourceValidator.validate(d);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertTrue(result.isSuccessful());
	}

	@Test
	public void testActivityDefinitionWithProcessAuthorizationRequesterRemoteOrganizationRecipientRemoteConsortiumRoleNotValid()
			throws Exception
	{
		ActivityDefinition d = createWithoutAuthExtension();
		d.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		Extension processAuthorization = d.addExtension()
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

		logResource(d);

		ValidationResult result = resourceValidator.validate(d);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertFalse(result.isSuccessful());
		assertEquals(9, result.getMessages().size());
	}

	@Test
	public void testActivityDefinitionWithoutProcessAuthorizationNotValid() throws Exception
	{
		ActivityDefinition d = createWithoutAuthExtension();
		d.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");

		logResource(d);

		ValidationResult result = resourceValidator.validate(d);

		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertFalse(result.isSuccessful());
		assertEquals(2, result.getMessages().size());
	}
}
