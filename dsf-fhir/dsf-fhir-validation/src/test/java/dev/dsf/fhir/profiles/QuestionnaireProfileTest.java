package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ResourceValidatorImpl;
import dev.dsf.fhir.validation.ValidationSupportRule;

public class QuestionnaireProfileTest
{
	private static final Logger logger = LoggerFactory.getLogger(QuestionnaireProfileTest.class);

	private static final String VERSION_1_0_0 = "1.0.0";
	private static final String VERSION_1_5_0 = "1.5.0";

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			Arrays.asList("dsf-questionnaire-1.0.0.xml", "dsf-questionnaire-1.5.0.xml"), Collections.emptyList(),
			Collections.emptyList());

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testQuestionnaireValidTypeString()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.STRING);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.STRING);
	}

	@Test
	public void testQuestionnaireValidTypeText()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.TEXT);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.TEXT);
	}

	@Test
	public void testQuestionnaireValidTypeInteger()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.INTEGER);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.INTEGER);
	}

	@Test
	public void testQuestionnaireValidTypeDecimal()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.DECIMAL);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.DECIMAL);
	}

	@Test
	public void testQuestionnaireValidTypeBoolean()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.BOOLEAN);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.BOOLEAN);
	}

	@Test
	public void testQuestionnaireValidTypeDate()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.DATE);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.DATE);
	}

	@Test
	public void testQuestionnaireValidTypeTime()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.TIME);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.TIME);
	}

	@Test
	public void testQuestionnaireValidTypeDateTime()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.DATETIME);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.DATETIME);
	}

	@Test
	public void testQuestionnaireValidTypeUrl()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.URL);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.URL);
	}

	@Test
	public void testQuestionnaireValidTypeReference()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.REFERENCE);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.REFERENCE);
	}

	@Test
	public void testQuestionnaireValidTypeDisplay()
	{
		testQuestionnaireValidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.DISPLAY);
		testQuestionnaireValidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.DISPLAY);
	}

	private void testQuestionnaireValidType(String profileVersion, Questionnaire.QuestionnaireItemType type)
	{
		Questionnaire res = createQuestionnaire(profileVersion, type);

		ValidationResult result = resourceValidator.validate(res);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testQuestionnaireInvalidTypeGroup()
	{
		testQuestionnaireInvalidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.GROUP);
		testQuestionnaireInvalidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.GROUP);
	}

	@Test
	public void testQuestionnaireInvalidTypeQuestion()
	{
		testQuestionnaireInvalidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.QUESTION);
		testQuestionnaireInvalidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.QUESTION);
	}

	@Test
	public void testQuestionnaireInvalidTypeChoice()
	{
		testQuestionnaireInvalidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.CHOICE);
		testQuestionnaireInvalidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.CHOICE);
	}

	@Test
	public void testQuestionnaireInvalidTypeOpenChoice()
	{
		testQuestionnaireInvalidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.OPENCHOICE);
		testQuestionnaireInvalidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.OPENCHOICE);
	}

	@Test
	public void testQuestionnaireInvalidTypeAttachment()
	{
		testQuestionnaireInvalidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.ATTACHMENT);
		testQuestionnaireInvalidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.ATTACHMENT);
	}

	@Test
	public void testQuestionnaireInvalidTypeQuantity()
	{
		testQuestionnaireInvalidType(VERSION_1_0_0, Questionnaire.QuestionnaireItemType.QUANTITY);
		testQuestionnaireInvalidType(VERSION_1_5_0, Questionnaire.QuestionnaireItemType.QUANTITY);
	}

	private void testQuestionnaireInvalidType(String profileVersion, Questionnaire.QuestionnaireItemType type)
	{
		Questionnaire res = createQuestionnaire(profileVersion, Questionnaire.QuestionnaireItemType.STRING);
		res.addItem().setLinkId("invalid-type").setType(type).setText("Invalid type");

		ValidationResult result = resourceValidator.validate(res);
		result.getMessages().stream().map(m -> m.getLocationString() + " " + m.getLocationLine() + ":"
				+ m.getLocationCol() + " - " + m.getSeverity() + ": " + m.getMessage()).forEach(logger::info);

		assertEquals(1,
				result.getMessages().stream()
						.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
								|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
						.filter(m -> m.getMessage() != null).filter(m -> m.getMessage().startsWith("type-code"))
						.count());
	}

	private Questionnaire createQuestionnaire(String profileVersion, Questionnaire.QuestionnaireItemType type)
	{
		Questionnaire res = new Questionnaire();
		res.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/questionnaire|" + profileVersion);
		res.setUrl("http://dsf.dev/fhir/Questionnaire/hello-world");
		res.setVersion("0.1.0");
		res.setDate(new Date());
		res.setStatus(Enumerations.PublicationStatus.ACTIVE);
		res.addItem().setLinkId("business-key").setType(Questionnaire.QuestionnaireItemType.STRING)
				.setText("The business-key of the process execution").setRequired(true);
		res.addItem().setLinkId("user-task-id").setType(Questionnaire.QuestionnaireItemType.STRING)
				.setText("The user-task-id of the process execution").setRequired(true);

		var item = res.addItem().setLinkId("valid-type").setType(type).setText("valid type");
		if (!Questionnaire.QuestionnaireItemType.DISPLAY.equals(type))
			item.setRequired(true);

		return res;
	}
}