package dev.dsf.fhir.profiles;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;

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

public class QuestionnaireProfileTest extends AbstractMetaTagProfileTest<Questionnaire>
{
	private static final Logger logger = LoggerFactory.getLogger(QuestionnaireProfileTest.class);

	@ClassRule
	public static final ValidationSupportRule validationRule = new ValidationSupportRule(
			List.of("dsf-extension-read-access-organization-2.0.0.xml",
					"dsf-extension-read-access-parent-organization-role-2.0.0.xml", "dsf-meta-2.0.0.xml",
					"dsf-questionnaire-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"),
			List.of("dsf-read-access-tag-2.0.0.xml", "dsf-organization-role-2.0.0.xml"));

	private final ResourceValidator resourceValidator = new ResourceValidatorImpl(validationRule.getFhirContext(),
			validationRule.getValidationSupport());

	@Test
	public void testQuestionnaireValidTypeString()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.STRING);
	}

	@Test
	public void testQuestionnaireValidTypeText()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.TEXT);
	}

	@Test
	public void testQuestionnaireValidTypeInteger()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.INTEGER);
	}

	@Test
	public void testQuestionnaireValidTypeDecimal()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.DECIMAL);
	}

	@Test
	public void testQuestionnaireValidTypeBoolean()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.BOOLEAN);
	}

	@Test
	public void testQuestionnaireValidTypeDate()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.DATE);
	}

	@Test
	public void testQuestionnaireValidTypeTime()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.TIME);
	}

	@Test
	public void testQuestionnaireValidTypeDateTime()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.DATETIME);
	}

	@Test
	public void testQuestionnaireValidTypeUrl()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.URL);
	}

	@Test
	public void testQuestionnaireValidTypeReference()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.REFERENCE);
	}

	@Test
	public void testQuestionnaireValidTypeDisplay()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.DISPLAY);
	}

	@Test
	public void testQuestionnaireValidTypeChoice()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.CHOICE);
	}

	@Test
	public void testQuestionnaireInvalidTypeQuantity()
	{
		testQuestionnaireValidType(Questionnaire.QuestionnaireItemType.QUANTITY);
	}

	private void testQuestionnaireValidType(Questionnaire.QuestionnaireItemType type)
	{
		Questionnaire res = createQuestionnaire(type);

		ValidationResult result = resourceValidator.validate(res);
		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertEquals(0, result.getMessages().stream().filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
				|| ResultSeverityEnum.FATAL.equals(m.getSeverity())).count());
	}

	@Test
	public void testQuestionnaireInvalidTypeGroup()
	{
		testQuestionnaireInvalidType(Questionnaire.QuestionnaireItemType.GROUP);
	}

	@Test
	public void testQuestionnaireInvalidTypeQuestion()
	{
		testQuestionnaireInvalidType(Questionnaire.QuestionnaireItemType.QUESTION);
	}

	@Test
	public void testQuestionnaireInvalidTypeOpenChoice()
	{
		testQuestionnaireInvalidType(Questionnaire.QuestionnaireItemType.OPENCHOICE);
	}

	@Test
	public void testQuestionnaireInvalidTypeAttachment()
	{
		testQuestionnaireInvalidType(Questionnaire.QuestionnaireItemType.ATTACHMENT);
	}

	private void testQuestionnaireInvalidType(Questionnaire.QuestionnaireItemType type)
	{
		Questionnaire q = createQuestionnaire(Questionnaire.QuestionnaireItemType.STRING);
		q.addItem().setLinkId("invalid-type").setType(type).setText("Invalid type");

		ValidationResult result = resourceValidator.validate(q);
		ValidationSupportRule.logValidationMessages(logger::debug, result);

		assertEquals(1,
				result.getMessages().stream()
						.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
								|| ResultSeverityEnum.FATAL.equals(m.getSeverity()))
						.filter(m -> m.getMessage() != null)
						.filter(m -> m.getMessage().startsWith("Constraint failed: type-code")).count());
	}

	private Questionnaire createQuestionnaire(Questionnaire.QuestionnaireItemType type)
	{
		Questionnaire q = create();
		q.getMeta().addTag().setSystem(CS_READ_ACCESS_TAG).setCode(TAG_ALL);

		var item = q.addItem().setLinkId("valid-type").setType(type).setText("valid type");
		if (!Questionnaire.QuestionnaireItemType.DISPLAY.equals(type))
			item.setRequired(true);

		return q;
	}

	@Override
	protected Questionnaire create()
	{
		Questionnaire q = new Questionnaire();
		q.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/questionnaire");
		q.setUrl("http://dsf.dev/fhir/Questionnaire/hello-world");
		q.setVersion("0.1.0");
		q.setDate(new Date());
		q.setStatus(Enumerations.PublicationStatus.ACTIVE);
		q.addItem().setLinkId("business-key").setType(Questionnaire.QuestionnaireItemType.STRING)
				.setText("The business-key of the process execution").setRequired(true);
		q.addItem().setLinkId("user-task-id").setType(Questionnaire.QuestionnaireItemType.STRING)
				.setText("The user-task-id of the process execution").setRequired(true);

		return q;
	}

	@Test
	public void runMetaTagTests() throws Exception
	{
		doRunMetaTagTests(resourceValidator);
	}
}