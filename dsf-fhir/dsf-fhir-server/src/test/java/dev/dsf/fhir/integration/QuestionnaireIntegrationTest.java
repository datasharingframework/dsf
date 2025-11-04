/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.integration;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
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
		Questionnaire questionnaire = createQuestionnaire();
		Questionnaire created = getWebserviceClient().create(questionnaire);

		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());
	}

	@Test
	public void testSearchByDate() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		Bundle searchBundle = getWebserviceClient().search(Questionnaire.class,
				Map.of("date", List.of("le2022-02-01")));

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
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		Bundle searchBundle = getWebserviceClient().search(Questionnaire.class,
				Map.of("identifier", List.of(TEST_IDENTIFIER_SYSTEM + "|" + TEST_IDENTIFIER_VALUE)));

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
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		Bundle searchBundle = getWebserviceClient().search(Questionnaire.class,
				Map.of("status", List.of(QUESTIONNAIRE_STATUS.toCode())));

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
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		Bundle searchBundle = getWebserviceClient().search(Questionnaire.class,
				Map.of("url", List.of(QUESTIONNAIRE_URL), "version", List.of(QUESTIONNAIRE_VERSION)));

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