package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
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
			List.of("dsf-questionnaire-response-1.0.0.xml"), List.of(), List.of());

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testQuestionnaireResponseValidTypeString()
	{
		testQuestionnaireResponseValidType(new StringType("foo"));
	}

	@Test
	public void testQuestionnaireResponseValidTypeInteger()
	{
		testQuestionnaireResponseValidType(new IntegerType(-1));
	}

	@Test
	public void testQuestionnaireResponseValidTypeDecimal()
	{
		testQuestionnaireResponseValidType(new DecimalType(-1));
	}

	@Test
	public void testQuestionnaireResponseValidTypeBoolean()
	{
		testQuestionnaireResponseValidType(new BooleanType(false));
	}

	@Test
	public void testQuestionnaireResponseValidTypeDate()
	{
		testQuestionnaireResponseValidType(new DateType("1900-01-01"));
	}

	@Test
	public void testQuestionnaireResponseValidTypeTime()
	{
		testQuestionnaireResponseValidType(new TimeType("00:00:00"));
	}

	@Test
	public void testQuestionnaireResponseValidTypeDateTime()
	{
		testQuestionnaireResponseValidType(new DateTimeType("1900-01-01T00:00:00.000Z"));
	}

	@Test
	public void testQuestionnaireResponseValidTypeUri()
	{
		testQuestionnaireResponseValidType(new UriType("http://example.de/foo"));
	}

	@Test
	public void testQuestionnaireResponseValidTypeReference()
	{
		testQuestionnaireResponseValidType(new Reference("Observation/foo"));
	}

	@Test
	public void testQuestionnaireResponseValidTypeReferenceWithBusinessKey()
	{
		testQuestionnaireResponseValidTypeWithBusinessKey(new Reference("Observation/foo"));
	}

	private void testQuestionnaireResponseValidTypeWithBusinessKey(Type type)
	{
		QuestionnaireResponse res = createQuestionnaireResponseWithBusinessKey(type);
		testQuestionnaireResponse(res);
	}

	private void testQuestionnaireResponseValidType(Type type)
	{
		QuestionnaireResponse res = createQuestionnaireResponse(type);
		testQuestionnaireResponse(res);
	}

	private void testQuestionnaireResponse(QuestionnaireResponse res)
	{
		ValidationResult result = resourceValidator.validate(res);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testQuestionnaireResponseInvalidType()
	{
		// TODO: activate after HAPI validator is fixed: https://github.com/hapifhir/org.hl7.fhir.core/issues/193

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
	}

	@Test
	public void testQuestionnaireResponseValidCompleted()
	{
		QuestionnaireResponse res = createQuestionnaireResponse(new StringType("foo"));
		res.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		res.setAuthored(new Date());
		res.setAuthor(new Reference().setIdentifier(
				new Identifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("foo.de")));

		ValidationResult result = resourceValidator.validate(res);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testQuestionnaireResponseInvalidCompletedNoAuthorAndNoAuthored()
	{
		QuestionnaireResponse res = createQuestionnaireResponse(new StringType("foo"));
		res.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);

		ValidationResult result = resourceValidator.validate(res);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(2,
				result.getMessages().stream()
						.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
								|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
						.filter(m -> m.getMessage() != null)
						.filter(m -> m.getMessage().startsWith("Constraint failed: authored-if-completed")
								|| m.getMessage().startsWith("Constraint failed: author-if-completed"))
						.count());
	}

	@Test
	public void testQuestionnaireResponseInvalidCompletedWithAuthorReferenceAndAuthored()
	{
		QuestionnaireResponse res = createQuestionnaireResponse(new StringType("foo"));
		res.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		res.setAuthored(new Date());
		res.setAuthor(new Reference("Organization/" + UUID.randomUUID().toString()));

		ValidationResult result = resourceValidator.validate(res);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(1,
				result.getMessages().stream()
						.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
								|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
						.filter(m -> m.getMessage() != null)
						.filter(m -> m.getMessage().startsWith("Constraint failed: author-if-completed")).count());
	}

	private QuestionnaireResponse createQuestionnaireResponseWithBusinessKey(Type type)
	{
		QuestionnaireResponse res = createQuestionnaireResponse(type);
		res.addItem().setLinkId("business-key").setText("The business-key of the process execution").addAnswer()
				.setValue(new StringType(UUID.randomUUID().toString()));

		return res;
	}

	private QuestionnaireResponse createQuestionnaireResponse(Type type)
	{
		QuestionnaireResponse res = new QuestionnaireResponse();
		res.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/questionnaire-response");
		res.setQuestionnaire("http://dsf.dev/fhir/Questionnaire/hello-world|0.1.0");
		res.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.INPROGRESS);
		res.addItem().setLinkId("user-task-id").setText("The user-task-id of the process execution").addAnswer()
				.setValue(new StringType("1"));
		res.addItem().setLinkId("valid-display").setText("valid-display");
		res.addItem().setLinkId("valid-answer").setText("valid answer").addAnswer().setValue(type);

		return res;
	}
}
