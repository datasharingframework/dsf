package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.TimeType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.UriType;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class QuestionnaireResponseProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(QuestionnaireResponseProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-questionnaire-response-2.0.0.xml", "dsf-extension-questionnaire-authorization-2.0.0.xml"),
			List.of("dsf-practitioner-role-1.0.0.xml"), List.of("dsf-practitioner-role-1.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testQuestionnaireResponseInProgressValidTypeString()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new StringType("foo"));
		testQuestionnaireResponse(r);

		Extension auth = r.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		auth.addExtension("practitioner", new Identifier().setSystem("http://dsf.dev/sid/practitioner-identifier")
				.setValue("practitioner1@organization.com"));
		auth.addExtension("practitioner", new Identifier().setSystem("http://dsf.dev/sid/practitioner-identifier")
				.setValue("practitioner2@organization.com"));
		auth.addExtension("practitioner-role",
				new Coding().setSystem("http://dsf.dev/fhir/CodeSystem/practitioner-role").setCode("DIC_USER"));
		auth.addExtension("practitioner-role",
				new Coding().setSystem("http://organization.com/fhir/CodeSystem/my-role").setCode("SOMETHING"));
		testQuestionnaireResponse(r);

		r.getAuthor().setType("Practitioner").getIdentifier().setSystem("http://dsf.dev/sid/practitioner-identifier")
				.setValue("practitioner@organization.com");
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseCompletedValidTypeString()
	{
		testQuestionnaireResponse(createQuestionnaireResponseCompleted(new StringType("foo"), null), 2);

		QuestionnaireResponse r = createQuestionnaireResponseCompleted(new StringType("foo"),
				"practitioner@organization.com");
		testQuestionnaireResponse(r);

		Extension auth = r.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		auth.addExtension("practitioner", new Identifier().setSystem("http://dsf.dev/sid/practitioner-identifier")
				.setValue("practitioner@organization.com"));
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeInteger()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new IntegerType(-1));

		r.getAuthor().setType("Practitioner").getIdentifier().setSystem("http://dsf.dev/sid/practitioner-identifier")
				.setValue("practitioner@organization.com");

		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeDecimal()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new DecimalType(-1));
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeBoolean()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new BooleanType(false));
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeDate()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new DateType("1900-01-01"));
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeTime()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new TimeType("00:00:00"));
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeDateTime()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new DateTimeType("1900-01-01T00:00:00.000Z"));
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeUri()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new UriType("http://example.de/foo"));
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeReference()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new Reference("Observation/foo"));
		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInProgressValidTypeReferenceWithBusinessKey()
	{
		QuestionnaireResponse r = createQuestionnaireInProgressResponseWithBusinessKey(
				new Reference("Observation/foo"));
		testQuestionnaireResponse(r);
	}

	private void testQuestionnaireResponse(QuestionnaireResponse r)
	{
		testQuestionnaireResponse(r, 0);
	}

	private void testQuestionnaireResponse(QuestionnaireResponse r, int numberOfExpectedErrors)
	{
		ValidationResult result = resourceValidator.validate(r);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(numberOfExpectedErrors,
				result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
						|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	// TODO: activate after HAPI validator is fixed: https://github.com/hapifhir/org.hl7.fhir.core/issues/193
	// @Test
	// public void testQuestionnaireResponseInvalidType()
	// {
	// QuestionnaireResponse res = createValidQuestionnaireResponse(new
	// Coding().setSystem("http://system.foo").setCode("code"));
	//
	// ValidationResult result = resourceValidator.validate(res);
	// result.getMessages().stream()
	// .map(m -> m.getLocationString() + " " + m.getLocationLine() + ":" + m.getLocationCol() + " - "
	// + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);
	//
	// assertEquals(1, result.getMessages().stream()
	// .filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity()) || ResultSeverityEnum.FATAL.equals(
	// m.getSeverity())).count());
	// }

	@Test
	public void testQuestionnaireResponseValidCompletedAuthorOrganization()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new StringType("foo"));
		r.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		r.setAuthored(new Date());
		r.setAuthor(new Reference().setType("Organization").setIdentifier(
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("foo.de")));

		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseValidCompletedAuthorPractitioner()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new StringType("foo"));
		r.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		r.setAuthored(new Date());
		r.setAuthor(new Reference().setType("Practitioner").setIdentifier(new Identifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue("practitioner@foo.de")));

		testQuestionnaireResponse(r);
	}

	@Test
	public void testQuestionnaireResponseInvalidCompletedNoAuthorAndNoAuthored()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new StringType("foo"));
		r.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);

		ValidationResult result = resourceValidator.validate(r);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(2, result.getMessages().stream()
				.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
						|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
				.filter(m -> m.getMessage() != null)
				.filter(m -> m.getMessage().startsWith("Constraint failed: authored-if-completed-or-amended")
						|| m.getMessage().startsWith("Constraint failed: author-if-completed-or-amended"))
				.count());
	}

	@Test
	public void testQuestionnaireResponseInvalidAmendedNoAuthorAndNoAuthored()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new StringType("foo"));
		r.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.AMENDED);

		ValidationResult result = resourceValidator.validate(r);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(2, result.getMessages().stream()
				.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
						|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
				.filter(m -> m.getMessage() != null)
				.filter(m -> m.getMessage().startsWith("Constraint failed: authored-if-completed-or-amended")
						|| m.getMessage().startsWith("Constraint failed: author-if-completed-or-amended"))
				.count());
	}

	@Test
	public void testQuestionnaireResponseInvalidCompletedWithAuthorReferenceAndAuthored()
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(new StringType("foo"));
		r.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		r.setAuthored(new Date());
		r.setAuthor(new Reference("Organization/" + UUID.randomUUID().toString()));

		ValidationResult result = resourceValidator.validate(r);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(1, result.getMessages().stream()
				.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
						|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
				.filter(m -> m.getMessage() != null)
				.filter(m -> m.getMessage().startsWith("Constraint failed: author-if-completed-or-amended")).count());
	}

	private QuestionnaireResponse createQuestionnaireInProgressResponseWithBusinessKey(Type type)
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(type);
		r.addItem().setLinkId("business-key").setText("The business-key of the process execution").addAnswer()
				.setValue(new StringType(UUID.randomUUID().toString()));

		return r;
	}

	private QuestionnaireResponse createQuestionnaireResponseCompleted(Type type, String practitionerIdentifierValue)
	{
		QuestionnaireResponse r = createQuestionnaireResponseInProgress(type);
		r.setStatus(QuestionnaireResponseStatus.COMPLETED);

		if (practitionerIdentifierValue != null)
		{
			r.setAuthored(new Date());
			r.getAuthor().setType("Practitioner").getIdentifier()
					.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(practitionerIdentifierValue);
		}

		return r;
	}

	private QuestionnaireResponse createQuestionnaireResponseInProgress(Type type)
	{
		QuestionnaireResponse r = new QuestionnaireResponse();
		r.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/questionnaire-response");
		r.setQuestionnaire("http://dsf.dev/fhir/Questionnaire/hello-world|0.1.0");
		r.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
		r.addItem().setLinkId("user-task-id").setText("The user-task-id of the process execution").addAnswer()
				.setValue(new StringType("1"));
		r.addItem().setLinkId("valid-display").setText("valid-display");
		r.addItem().setLinkId("valid-answer").setText("valid answer").addAnswer().setValue(type);

		return r;
	}
}
