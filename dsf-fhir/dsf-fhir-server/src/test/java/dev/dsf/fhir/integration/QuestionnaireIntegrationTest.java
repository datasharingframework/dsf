package dev.dsf.fhir.integration;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.Test;

import dev.dsf.fhir.dao.QuestionnaireDao;

public class QuestionnaireIntegrationTest extends AbstractQuestionnaireIntegrationTest
{
	@Test
	public void testCreateValidByLocalUser()
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion100();
		Questionnaire created = getWebserviceClient().create(questionnaire);

		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());
	}

	@Test
	public void testSearchByDate() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion100();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		Bundle searchBundle = getWebserviceClient().search(Questionnaire.class,
				Map.of("date", Collections.singletonList("le2022-02-01")));

		assertNotNull(searchBundle.getEntry());
		assertEquals(1, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof Questionnaire);

		Questionnaire searchQuestionnaire = (Questionnaire) searchBundle.getEntry().get(0).getResource();
		assertTrue(searchQuestionnaire.hasDate());
		assertEquals(0, QUESTIONNAIRE_DATE.compareTo(searchQuestionnaire.getDate()));
	}

	@Test
	public void testSearchByIdentifier() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion100();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		Bundle searchBundle = getWebserviceClient().search(Questionnaire.class,
				Map.of("identifier", Collections.singletonList(TEST_IDENTIFIER_SYSTEM + "|" + TEST_IDENTIFIER_VALUE)));

		assertNotNull(searchBundle.getEntry());
		assertEquals(1, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof Questionnaire);

		Questionnaire searchQuestionnaire = (Questionnaire) searchBundle.getEntry().get(0).getResource();
		assertEquals(1, searchQuestionnaire.getIdentifier().size());
		assertEquals(TEST_IDENTIFIER_SYSTEM, searchQuestionnaire.getIdentifier().get(0).getSystem());
		assertEquals(TEST_IDENTIFIER_VALUE, searchQuestionnaire.getIdentifier().get(0).getValue());
	}

	@Test
	public void testSearchByStatus() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion100();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		Bundle searchBundle = getWebserviceClient().search(Questionnaire.class,
				Map.of("status", Collections.singletonList(QUESTIONNAIRE_STATUS.toCode())));

		assertNotNull(searchBundle.getEntry());
		assertEquals(1, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof Questionnaire);

		Questionnaire searchQuestionnaire = (Questionnaire) searchBundle.getEntry().get(0).getResource();
		assertTrue(searchQuestionnaire.hasStatus());
		assertEquals(QUESTIONNAIRE_STATUS, searchQuestionnaire.getStatus());
	}

	@Test
	public void testSearchByUrlAndVersion() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion100();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		Bundle searchBundle = getWebserviceClient().search(Questionnaire.class,
				Map.of("url", Collections.singletonList(QUESTIONNAIRE_URL), "version",
						Collections.singletonList(QUESTIONNAIRE_VERSION)));

		assertNotNull(searchBundle.getEntry());
		assertEquals(1, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof Questionnaire);

		Questionnaire searchQuestionnaire = (Questionnaire) searchBundle.getEntry().get(0).getResource();
		assertTrue(searchQuestionnaire.hasUrl());
		assertEquals(QUESTIONNAIRE_URL, searchQuestionnaire.getUrl());
		assertTrue(searchQuestionnaire.hasVersion());
		assertEquals(QUESTIONNAIRE_VERSION, searchQuestionnaire.getVersion());
	}
}
