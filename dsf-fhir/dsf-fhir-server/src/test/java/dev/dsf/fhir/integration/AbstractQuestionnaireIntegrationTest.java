package dev.dsf.fhir.integration;

import static org.junit.Assert.assertNotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;

import dev.dsf.fhir.authentication.OrganizationProvider;

public class AbstractQuestionnaireIntegrationTest extends AbstractIntegrationTest
{
	protected static final String QUESTIONNAIRE_URL = "http://dsf.dev/fhir/Questionnaire/userTask/foo";
	protected static final String QUESTIONNAIRE_VERSION = "2.0";
	protected static final String QUESTIONNAIRE_URL_VERSION = QUESTIONNAIRE_URL + "|" + QUESTIONNAIRE_VERSION;
	protected static final String TEST_IDENTIFIER_SYSTEM = "http://dsf.dev/fhir/CodeSystem/test";
	protected static final String TEST_IDENTIFIER_VALUE = "foo";
	protected static final Enumerations.PublicationStatus QUESTIONNAIRE_STATUS = Enumerations.PublicationStatus.ACTIVE;
	protected static final Date QUESTIONNAIRE_DATE = Date
			.from(LocalDateTime.parse("2022-01-01T00:00:00").toInstant(ZoneOffset.UTC));
	protected static final String QUESTIONNAIRE_ITEM_USER_TASK_ID_LINK = "user-task-id";
	protected static final String QUESTIONNAIRE_ITEM_USER_TASK_ID_TEXT = "The user-task-id of the process execution";
	protected static final String QUESTIONNAIRE_ITEM_BUSINESS_KEY_LINK = "business-key";
	protected static final String QUESTIONNAIRE_ITEM_BUSINESS_KEY_TEXT = "The business-key of the process execution";

	protected static final Date QUESTIONNAIRE_RESPONSE_DATE = Date
			.from(LocalDateTime.parse("2022-01-02T00:00:00").toInstant(ZoneOffset.UTC));

	protected Questionnaire createQuestionnaire()
	{
		Questionnaire questionnaire = new Questionnaire();
		questionnaire.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/questionnaire");
		questionnaire.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");

		questionnaire.setUrl(QUESTIONNAIRE_URL);
		questionnaire.setVersion(QUESTIONNAIRE_VERSION);

		questionnaire.addIdentifier().setSystem(TEST_IDENTIFIER_SYSTEM).setValue(TEST_IDENTIFIER_VALUE);

		questionnaire.setStatus(QUESTIONNAIRE_STATUS);
		questionnaire.setDate(QUESTIONNAIRE_DATE);

		questionnaire.addItem().setLinkId(QUESTIONNAIRE_ITEM_USER_TASK_ID_LINK)
				.setText(QUESTIONNAIRE_ITEM_USER_TASK_ID_TEXT).setType(Questionnaire.QuestionnaireItemType.STRING)
				.setRequired(true);
		questionnaire.addItem().setLinkId(QUESTIONNAIRE_ITEM_BUSINESS_KEY_LINK)
				.setText(QUESTIONNAIRE_ITEM_BUSINESS_KEY_TEXT).setType(Questionnaire.QuestionnaireItemType.STRING)
				.setRequired(true);

		return questionnaire;
	}

	protected QuestionnaireResponse createQuestionnaireResponse()
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		assertNotNull(organizationProvider);

		QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();
		questionnaireResponse.getMeta().addProfile("http://dsf.dev/fhir/StructureDefinition/questionnaire-response");

		questionnaireResponse.getIdentifier().setSystem(TEST_IDENTIFIER_SYSTEM).setValue(TEST_IDENTIFIER_VALUE);
		questionnaireResponse.setQuestionnaire(QUESTIONNAIRE_URL_VERSION);

		questionnaireResponse.setStatus(QuestionnaireResponseStatus.INPROGRESS);
		questionnaireResponse.setAuthored(QUESTIONNAIRE_RESPONSE_DATE);

		questionnaireResponse.getAuthor().setType(ResourceType.Organization.name())
				.setReferenceElement(organizationProvider.getLocalOrganization().get().getIdElement().toVersionless())
				.getIdentifier().setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");

		addItem(questionnaireResponse, QUESTIONNAIRE_ITEM_USER_TASK_ID_LINK, QUESTIONNAIRE_ITEM_USER_TASK_ID_TEXT,
				new StringType(UUID.randomUUID().toString()));
		addItem(questionnaireResponse, QUESTIONNAIRE_ITEM_BUSINESS_KEY_LINK, QUESTIONNAIRE_ITEM_BUSINESS_KEY_TEXT,
				new StringType(UUID.randomUUID().toString()));

		return questionnaireResponse;
	}

	protected void addItem(QuestionnaireResponse questionnaireResponse, String linkId, String text, Type answer)
	{
		List<QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent> answerComponent = Collections
				.singletonList(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(answer));

		questionnaireResponse.addItem().setLinkId(linkId).setText(text).setAnswer(answerComponent);
	}
}