package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.StringType;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class OrganizationProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-organization-1.0.0.xml", "dsf-organization-parent-1.0.0.xml",
					"dsf-extension-certificate-thumbprint-1.0.0.xml", "dsf-endpoint-1.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-1.0.0.xml",
					"dsf-extension-read-access-organization-1.0.0.xml"),
			List.of("dsf-read-access-tag-1.0.0.xml"), List.of("dsf-read-access-tag-1.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testOrganizationProfileValid() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization");
		Coding tag = org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag")
				.setCode("ORGANIZATION");
		tag.addExtension("http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization",
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("organization.com"));
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("test.org");
		org.setActive(true);
		org.addEndpoint().setReference("Endpoint/" + UUID.randomUUID().toString());
		org.addEndpoint().setReference("Endpoint/" + UUID.randomUUID().toString());
		org.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(
						"6d40e7c82ead96a9c5851976002d3732631d6e0e82e10e98c5ba568b2980b45a4436577d329ee47a8bc50fd35e39aa3c54faa23249d7b7a82a117824a4c430eb"));

		ValidationResult result = resourceValidator.validate(org);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testOrganizationParentProfileValid() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization-parent");
		org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("parent.org");
		org.setActive(true);

		ValidationResult result = resourceValidator.validate(org);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testOrganizationProfileNotValidMissingEndpoint() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization");
		Coding tag = org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag")
				.setCode("ORGANIZATION");
		tag.addExtension("http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization",
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("organization.com"));
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("test.org");
		org.setActive(true);
		org.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(
						"6d40e7c82ead96a9c5851976002d3732631d6e0e82e10e98c5ba568b2980b45a4436577d329ee47a8bc50fd35e39aa3c54faa23249d7b7a82a117824a4c430eb"));

		ValidationResult result = resourceValidator.validate(org);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(1, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testOrganizationProfileNotValidMissingIdentifier() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization");
		org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		org.setActive(true);
		org.addEndpoint().setReference("Endpoint/" + UUID.randomUUID().toString());
		org.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(
						"6d40e7c82ead96a9c5851976002d3732631d6e0e82e10e98c5ba568b2980b45a4436577d329ee47a8bc50fd35e39aa3c54faa23249d7b7a82a117824a4c430eb"));

		ValidationResult result = resourceValidator.validate(org);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(3, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testOrganizationProfileNotValidMissingActive() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization");
		org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("test.org");
		org.addEndpoint().setReference("Endpoint/" + UUID.randomUUID().toString());
		org.addExtension().setUrl("http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint")
				.setValue(new StringType(
						"6d40e7c82ead96a9c5851976002d3732631d6e0e82e10e98c5ba568b2980b45a4436577d329ee47a8bc50fd35e39aa3c54faa23249d7b7a82a117824a4c430eb"));

		ValidationResult result = resourceValidator.validate(org);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(1, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testOrganizationProfileNotValidMissingThumbprint() throws Exception
	{
		Organization org = new Organization();
		org.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/organization");
		org.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		org.addIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("test.org");
		org.setActive(true);
		org.addEndpoint().setReference("Endpoint/" + UUID.randomUUID().toString());

		ValidationResult result = resourceValidator.validate(org);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(1, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}
}
