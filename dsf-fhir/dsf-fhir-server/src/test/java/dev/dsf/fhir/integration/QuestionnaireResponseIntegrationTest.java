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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

import dev.dsf.fhir.authentication.OrganizationProvider;
import dev.dsf.fhir.dao.QuestionnaireDao;
import dev.dsf.fhir.dao.QuestionnaireResponseDao;

public class QuestionnaireResponseIntegrationTest extends AbstractQuestionnaireIntegrationTest
{
	@Test
	public void testCreateValidByLocalUser() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponse created = getWebserviceClient().create(questionnaireResponse);

		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());
	}

	@Test
	public void testCreateNotAllowedByLocalUserStatusCompleted() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		questionnaireResponse.setStatus(QuestionnaireResponseStatus.COMPLETED);

		expectForbidden(() -> getWebserviceClient().create(questionnaireResponse));
	}

	@Test
	public void testCreateNotAllowedByLocalUserQuestionnaireDoesNotExists() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();

		expectForbidden(() -> getWebserviceClient().create(questionnaireResponse));
	}

	@Test
	public void testCreateNotAllowedByRemoteUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();

		expectForbidden(() -> getExternalWebserviceClient().create(questionnaireResponse));
	}

	@Test
	public void testUpdateAllowedByLocalUser() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		QuestionnaireResponse updated = getWebserviceClient().update(created);

		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), updated.getIdElement().getIdPart());
		assertNotNull(updated.getIdElement().getVersionIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());
	}

	@Test
	public void testUpdateNotAllowedByLocalUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		expectForbidden(() -> getWebserviceClient().update(created));
	}

	@Test
	public void testUpdateNotAllowedByLocalUserNoUserTaskId() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		created.setStatus(QuestionnaireResponseStatus.STOPPED);
		created.getItem().clear();

		expectForbidden(() -> getWebserviceClient().update(created));
	}

	@Test
	public void testUpdateNotAllowedByLocalUserChangedUserTaskId() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		created.setStatus(QuestionnaireResponseStatus.STOPPED);
		created.getItem().clear();

		addItem(created, QUESTIONNAIRE_ITEM_USER_TASK_ID_LINK, QUESTIONNAIRE_ITEM_USER_TASK_ID_TEXT,
				new StringType(UUID.randomUUID().toString()));

		expectForbidden(() -> getWebserviceClient().update(created));
	}

	@Test
	public void testSecondUpdateNotAllowedByLocalUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);
		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		QuestionnaireResponse updated = questionnaireResponseDao.update(created);

		expectForbidden(() -> getWebserviceClient().update(updated));
	}

	@Test
	public void testUpdateNotAllowedByRemoteUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);

		expectForbidden(() -> getExternalWebserviceClient().update(created));
	}

	@Test
	public void testSearchByDate() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		Bundle searchBundle = getWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("authored", List.of("le2022-02-01")));

		assertNotNull(searchBundle.getEntry());
		assertEquals(1, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse searchQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(searchQuestionnaireResponse.hasAuthored());
		assertEquals(0, QUESTIONNAIRE_RESPONSE_DATE.compareTo(searchQuestionnaireResponse.getAuthored()));
	}

	@Test
	public void testSearchByIdentifier() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		Bundle searchBundle = getWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("identifier", List.of(TEST_IDENTIFIER_SYSTEM + "|" + TEST_IDENTIFIER_VALUE)));

		assertNotNull(searchBundle.getEntry());
		assertEquals(1, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse foundQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(foundQuestionnaireResponse.hasIdentifier());
		assertEquals(TEST_IDENTIFIER_SYSTEM, foundQuestionnaireResponse.getIdentifier().getSystem());
		assertEquals(TEST_IDENTIFIER_VALUE, foundQuestionnaireResponse.getIdentifier().getValue());
	}

	@Test
	public void testSearchByIdentifierRemoteUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		Bundle searchBundle = getExternalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("identifier", List.of(TEST_IDENTIFIER_SYSTEM + "|" + TEST_IDENTIFIER_VALUE)));

		assertNotNull(searchBundle.getEntry());
		assertEquals(0, searchBundle.getEntry().size());
	}

	@Test
	public void testSearchByQuestionnaireWithVersion() throws Exception
	{
		testSearchByQuestionnaire(QUESTIONNAIRE_URL_VERSION);
	}

	@Test
	public void testSearchByQuestionnaireWithoutVersion() throws Exception
	{
		testSearchByQuestionnaire(QUESTIONNAIRE_URL);
	}

	private void testSearchByQuestionnaire(String questionnaireUrl) throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		Bundle searchBundle = getWebserviceClient().search(QuestionnaireResponse.class, Map.of("questionnaire",
				List.of(questionnaireUrl), "_include", List.of("QuestionnaireResponse:questionnaire")));

		assertNotNull(searchBundle.getEntry());
		assertEquals(2, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse searchQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(searchQuestionnaireResponse.hasQuestionnaire());
		assertEquals(QUESTIONNAIRE_URL_VERSION, searchQuestionnaireResponse.getQuestionnaire());

		assertNotNull(searchBundle.getEntry().get(1));
		assertNotNull(searchBundle.getEntry().get(1).getResource());
		assertTrue(searchBundle.getEntry().get(1).getResource() instanceof Questionnaire);

		Questionnaire searchQuestionnaire = (Questionnaire) searchBundle.getEntry().get(1).getResource();
		assertTrue(searchQuestionnaire.hasUrl());
		assertEquals(QUESTIONNAIRE_URL, searchQuestionnaire.getUrl());
		assertTrue(searchQuestionnaire.hasVersion());
		assertEquals(QUESTIONNAIRE_VERSION, searchQuestionnaire.getVersion());
	}

	@Test
	public void testSearchByQuestionnaireNoVersion() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire().setVersion(null);
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse().setQuestionnaire(QUESTIONNAIRE_URL);
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		Bundle searchBundle = getWebserviceClient().search(QuestionnaireResponse.class, Map.of("questionnaire",
				List.of(QUESTIONNAIRE_URL), "_include", List.of("QuestionnaireResponse:questionnaire")));

		assertNotNull(searchBundle.getEntry());
		assertEquals(2, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse searchQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(searchQuestionnaireResponse.hasQuestionnaire());
		assertEquals(QUESTIONNAIRE_URL, searchQuestionnaireResponse.getQuestionnaire());

		assertNotNull(searchBundle.getEntry().get(1));
		assertNotNull(searchBundle.getEntry().get(1).getResource());
		assertTrue(searchBundle.getEntry().get(1).getResource() instanceof Questionnaire);

		Questionnaire searchQuestionnaire = (Questionnaire) searchBundle.getEntry().get(1).getResource();
		assertTrue(searchQuestionnaire.hasUrl());
		assertEquals(QUESTIONNAIRE_URL, searchQuestionnaire.getUrl());
		assertFalse(searchQuestionnaire.hasVersion());
	}

	@Test
	public void testSearchByQuestionnaireWithoutVersionButMultipleVersionExist() throws Exception
	{
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);

		Questionnaire questionnaire1 = createQuestionnaire().setVersion("0.1.0");
		questionnaireDao.create(questionnaire1);

		Questionnaire questionnaire2 = createQuestionnaire();
		questionnaireDao.create(questionnaire2);

		Questionnaire questionnaire3 = createQuestionnaire().setVersion("0.2.0");
		questionnaireDao.create(questionnaire3);

		Questionnaire questionnaire4 = createQuestionnaire().setVersion(null);
		questionnaireDao.create(questionnaire4);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		Bundle searchBundle = getWebserviceClient().search(QuestionnaireResponse.class, Map.of("questionnaire",
				List.of(QUESTIONNAIRE_URL), "_include", List.of("QuestionnaireResponse:questionnaire")));

		assertNotNull(searchBundle.getEntry());
		assertEquals(2, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse searchQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(searchQuestionnaireResponse.hasQuestionnaire());
		assertEquals(QUESTIONNAIRE_URL_VERSION, searchQuestionnaireResponse.getQuestionnaire());

		assertNotNull(searchBundle.getEntry().get(1));
		assertNotNull(searchBundle.getEntry().get(1).getResource());
		assertTrue(searchBundle.getEntry().get(1).getResource() instanceof Questionnaire);

		Questionnaire searchQuestionnaire = (Questionnaire) searchBundle.getEntry().get(1).getResource();
		assertTrue(searchQuestionnaire.hasUrl());
		assertEquals(QUESTIONNAIRE_URL, searchQuestionnaire.getUrl());

		// Expect newest version 1.0.0 (null, 0.1.0 and 0.2.0 exist as well)
		assertTrue(searchQuestionnaire.hasVersion());
		assertEquals(QUESTIONNAIRE_VERSION, searchQuestionnaire.getVersion());
	}

	@Test
	public void testSearchByStatus() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		Bundle searchBundle = getWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("status", List.of(QuestionnaireResponseStatus.INPROGRESS.toCode())));

		assertNotNull(searchBundle.getEntry());
		assertEquals(1, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse searchQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(searchQuestionnaireResponse.hasStatus());
		assertEquals(QuestionnaireResponseStatus.INPROGRESS, searchQuestionnaireResponse.getStatus());
	}

	@Test
	public void testSearchBySubjectReference() throws Exception
	{
		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		Organization localOrganization = organizationProvider.getLocalOrganization().get();

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		questionnaireResponse.getSubject().setType(ResourceType.Organization.name())
				.setReferenceElement(localOrganization.getIdElement().toVersionless()).getIdentifier()
				.setSystem("http://dsf.dev/sid/organization-identifier").setValue("Test_Organization");

		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		Bundle searchBundle = getWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("subject", List.of(localOrganization.getIdElement().toUnqualifiedVersionless().toString()),
						"_include", List.of("QuestionnaireResponse:subject:Organization")));

		assertNotNull(searchBundle.getEntry());
		assertEquals(2, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse searchQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(searchQuestionnaireResponse.hasStatus());
		assertEquals(localOrganization.getIdentifierFirstRep().getSystem(),
				searchQuestionnaireResponse.getSubject().getIdentifier().getSystem());
		assertEquals(localOrganization.getIdentifierFirstRep().getValue(),
				searchQuestionnaireResponse.getSubject().getIdentifier().getValue());
		assertNull(searchQuestionnaireResponse.getSubject().getReference());

		assertNotNull(searchBundle.getEntry().get(1));
		assertNotNull(searchBundle.getEntry().get(1).getResource());
		assertTrue(searchBundle.getEntry().get(1).getResource() instanceof Organization);

		Organization searchOrganization = (Organization) searchBundle.getEntry().get(1).getResource();
		assertEquals(localOrganization.getIdentifierFirstRep().getSystem(),
				searchOrganization.getIdentifierFirstRep().getSystem());
		assertEquals(localOrganization.getIdentifierFirstRep().getValue(),
				searchOrganization.getIdentifierFirstRep().getValue());
	}

	@Test
	public void testDeleteAllowedByLocalUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		getWebserviceClient().delete(QuestionnaireResponse.class, created.getIdElement().getIdPart());
	}

	@Test
	public void testDeleteNotAllowedByRemoteUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		expectForbidden(() -> getExternalWebserviceClient().delete(QuestionnaireResponse.class,
				created.getIdElement().getIdPart()));
	}

	@Test
	public void testReadAllowedByLocalUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		QuestionnaireResponse read = getWebserviceClient().read(QuestionnaireResponse.class,
				created.getIdElement().getIdPart());

		assertNotNull(read);
		assertNotNull(read.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), read.getIdElement().getIdPart());
		assertNotNull(read.getIdElement().getVersionIdPart());
		assertEquals("1", read.getIdElement().getVersionIdPart());
	}

	@Test
	public void testReadNotAllowedByRemoteUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		expectForbidden(() -> getExternalWebserviceClient().read(QuestionnaireResponse.class,
				created.getIdElement().getIdPart()));
	}

	@Test
	public void testReadNotAllowedByRemoteUserWithVersion() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		expectForbidden(() -> getExternalWebserviceClient().read(QuestionnaireResponse.class,
				created.getIdElement().getIdPart(), created.getIdElement().getVersionIdPart()));
	}

	@Test
	public void testNotModifiedCheckAllowedByLocalUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		QuestionnaireResponse read = getWebserviceClient().read(created);
		assertNotNull(read);
		assertTrue(created == read);
	}

	@Test
	public void testNotModifiedCheckNotAllowedByRemoteUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		expectForbidden(() -> getExternalWebserviceClient().read(created));
	}

	@Test
	public void testNotModifiedCheckAllowedByLocalUserWithModification() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);
		QuestionnaireResponse updated = questionnaireResponseDao.update(created);
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		QuestionnaireResponse read = getWebserviceClient().read(created);
		assertNotNull(read);
		assertTrue(created != read);

		assertEquals("2", read.getIdElement().getVersionIdPart());
	}

	@Test
	public void testNotModifiedCheckNotAllowedByRemoteUserWithModification() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);
		QuestionnaireResponse updated = questionnaireResponseDao.update(created);
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		expectForbidden(() -> getExternalWebserviceClient().read(created));
	}

	@Test
	public void testHistory() throws Exception
	{
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(createQuestionnaireResponse());

		Bundle historyBundle = getWebserviceClient().history(QuestionnaireResponse.class,
				created.getIdElement().getIdPart());

		assertNotNull(historyBundle.getEntry());
		assertEquals(1, historyBundle.getEntry().size());
		assertNotNull(historyBundle.getEntry().get(0));
		assertNotNull(historyBundle.getEntry().get(0).getResource());
		assertTrue(historyBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		Bundle historyBundle2 = getWebserviceClient().history(QuestionnaireResponse.class);

		assertNotNull(historyBundle2.getEntry());
		assertEquals(1, historyBundle2.getEntry().size());
		assertNotNull(historyBundle2.getEntry().get(0));
		assertNotNull(historyBundle2.getEntry().get(0).getResource());
		assertTrue(historyBundle2.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		Bundle historyBundle3 = getWebserviceClient().history(1, Integer.MAX_VALUE);

		assertNotNull(historyBundle3.getEntry());

		List<QuestionnaireResponse> qrFromBundle = historyBundle3.getEntry().stream()
				.filter(e -> e.hasResource() && e.getResource() instanceof QuestionnaireResponse)
				.map(e -> (QuestionnaireResponse) e.getResource()).toList();

		assertEquals(1, qrFromBundle.size());
		assertNotNull(qrFromBundle.get(0));
	}

	@Test
	public void testHistoryRemoteUser() throws Exception
	{
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(createQuestionnaireResponse());

		Bundle historyBundle = getExternalWebserviceClient().history(QuestionnaireResponse.class,
				created.getIdElement().getIdPart());

		assertNotNull(historyBundle.getEntry());
		assertEquals(0, historyBundle.getEntry().size());

		Bundle historyBundle2 = getExternalWebserviceClient().history(QuestionnaireResponse.class);

		assertNotNull(historyBundle2.getEntry());
		assertEquals(0, historyBundle2.getEntry().size());

		Bundle historyBundle3 = getExternalWebserviceClient().history(1, Integer.MAX_VALUE);

		assertNotNull(historyBundle3.getEntry());
		assertNotSame(0, historyBundle3.getEntry().size());

		List<QuestionnaireResponse> qrFromBundle = historyBundle3.getEntry().stream()
				.filter(e -> e.hasResource() && e.getResource() instanceof QuestionnaireResponse)
				.map(e -> (QuestionnaireResponse) e.getResource()).toList();

		assertEquals(0, qrFromBundle.size());
	}

	@Test
	public void testDeletePermanentlyAllowedByLocalUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);
		questionnaireResponseDao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		getWebserviceClient().deletePermanently(QuestionnaireResponse.class, created.getIdElement().getIdPart());
	}

	@Test
	public void testDeletePermanentlyNotAllowedByRemoteUser() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);
		questionnaireResponseDao.delete(UUID.fromString(created.getIdElement().getIdPart()));

		expectForbidden(() -> getExternalWebserviceClient().deletePermanently(QuestionnaireResponse.class,
				created.getIdElement().getIdPart()));
	}

	@Test
	public void testUpdateAllowedByMinimalUserWithRole() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		Extension authExtension = questionnaireResponse.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		authExtension.addExtension().setUrl("practitioner-role")
				.setValue(new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));

		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		created.setAuthored(new Date());
		created.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);

		QuestionnaireResponse updated = getMinimalWebserviceClient().update(created);
		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), updated.getIdElement().getIdPart());
		assertNotNull(updated.getIdElement().getVersionIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());
	}

	@Test
	public void testUpdateNotAllowedByMinimalUserWithoutRole() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		Extension authExtension = questionnaireResponse.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		authExtension.addExtension().setUrl("practitioner-role")
				.setValue(new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "UAC_USER", null));

		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		created.setAuthored(new Date());
		created.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);

		expectForbidden(() -> getMinimalWebserviceClient().update(created));
	}

	@Test
	public void testReadNotAllowedByPractitionerUserWithoutRole() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		Extension authExtension = questionnaireResponse.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		authExtension.addExtension().setUrl("practitioner-role")
				.setValue(new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "UAC_USER", null));

		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		expectForbidden(() -> getMinimalWebserviceClient().read(QuestionnaireResponse.class,
				created.getIdElement().getIdPart(), created.getIdElement().getVersionIdPart()));

		Bundle searchResult1a = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult1a);
		assertEquals(0, searchResult1a.getTotal());
		assertEquals(0, searchResult1a.getEntry().size());

		Bundle searchResult1b = getWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult1b);
		assertEquals(1, searchResult1b.getTotal());
		assertEquals(1, searchResult1b.getEntry().size());

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		created.setAuthored(new Date());
		created.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.ADMIN_CLIENT_MAIL);

		QuestionnaireResponse updated = getAdminWebserviceClient().update(created);
		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), updated.getIdElement().getIdPart());
		assertNotNull(updated.getIdElement().getVersionIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		expectForbidden(() -> getMinimalWebserviceClient().read(QuestionnaireResponse.class,
				updated.getIdElement().getIdPart(), updated.getIdElement().getVersionIdPart()));

		Bundle searchResult2a = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult2a);
		assertEquals(0, searchResult2a.getTotal());
		assertEquals(0, searchResult2a.getEntry().size());

		Bundle searchResult2b = getAdminWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult2b);
		assertEquals(1, searchResult2b.getTotal());
		assertEquals(1, searchResult2b.getEntry().size());
	}

	@Test
	public void testReadAllowedByPractitionerUserWithRole() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		Extension authExtension = questionnaireResponse.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		authExtension.addExtension().setUrl("practitioner-role")
				.setValue(new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));

		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		QuestionnaireResponse read1 = getMinimalWebserviceClient().read(QuestionnaireResponse.class,
				created.getIdElement().getIdPart(), created.getIdElement().getVersionIdPart());
		assertNotNull(read1);
		assertNotNull(read1.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), read1.getIdElement().getIdPart());
		assertNotNull(read1.getIdElement().getVersionIdPart());
		assertEquals("1", read1.getIdElement().getVersionIdPart());

		Bundle searchResult1a = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult1a);
		assertEquals(1, searchResult1a.getTotal());
		assertEquals(1, searchResult1a.getEntry().size());

		Bundle searchResult1b = getWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult1b);
		assertEquals(1, searchResult1b.getTotal());
		assertEquals(1, searchResult1b.getEntry().size());

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		created.setAuthored(new Date());
		created.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);

		QuestionnaireResponse updated = getMinimalWebserviceClient().update(created);
		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), updated.getIdElement().getIdPart());
		assertNotNull(updated.getIdElement().getVersionIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		QuestionnaireResponse read2 = getMinimalWebserviceClient().read(QuestionnaireResponse.class,
				updated.getIdElement().getIdPart(), updated.getIdElement().getVersionIdPart());
		assertNotNull(read2);
		assertNotNull(read2.getIdElement().getIdPart());
		assertEquals(updated.getIdElement().getIdPart(), read2.getIdElement().getIdPart());
		assertNotNull(read2.getIdElement().getVersionIdPart());
		assertEquals("2", read2.getIdElement().getVersionIdPart());

		Bundle searchResult2a = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult2a);
		assertEquals(1, searchResult2a.getTotal());
		assertEquals(1, searchResult2a.getEntry().size());

		Bundle searchResult2b = getAdminWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult2b);
		assertEquals(1, searchResult2b.getTotal());
		assertEquals(1, searchResult2b.getEntry().size());
	}

	@Test
	public void testReadNotAllowedByPractitionerUserWithoutIdentifier() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		Extension authExtension = questionnaireResponse.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		authExtension.addExtension().setUrl("practitioner")
				.setValue(new Identifier().setSystem("http://dsf.dev/sid/practitioner-identifier")
						.setValue(X509Certificates.PRACTITIONER_CLIENT_MAIL));

		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		expectForbidden(() -> getMinimalWebserviceClient().read(QuestionnaireResponse.class,
				created.getIdElement().getIdPart(), created.getIdElement().getVersionIdPart()));

		Bundle searchResult1a = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult1a);
		assertEquals(0, searchResult1a.getTotal());
		assertEquals(0, searchResult1a.getEntry().size());

		Bundle searchResult1b = getWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult1b);
		assertEquals(1, searchResult1b.getTotal());
		assertEquals(1, searchResult1b.getEntry().size());

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		created.setAuthored(new Date());
		created.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.ADMIN_CLIENT_MAIL);

		QuestionnaireResponse updated = getAdminWebserviceClient().update(created);
		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), updated.getIdElement().getIdPart());
		assertNotNull(updated.getIdElement().getVersionIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		expectForbidden(() -> getMinimalWebserviceClient().read(QuestionnaireResponse.class,
				updated.getIdElement().getIdPart(), updated.getIdElement().getVersionIdPart()));

		Bundle searchResult2a = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult2a);
		assertEquals(0, searchResult2a.getTotal());
		assertEquals(0, searchResult2a.getEntry().size());

		Bundle searchResult2b = getAdminWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult2b);
		assertEquals(1, searchResult2b.getTotal());
		assertEquals(1, searchResult2b.getEntry().size());
	}

	@Test
	public void testReadAllowedByPractitionerUserWithIdentifier() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		Extension authExtension = questionnaireResponse.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		authExtension.addExtension().setUrl("practitioner")
				.setValue(new Identifier().setSystem("http://dsf.dev/sid/practitioner-identifier")
						.setValue(X509Certificates.MINIMAL_CLIENT_MAIL));

		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		QuestionnaireResponse read1 = getMinimalWebserviceClient().read(QuestionnaireResponse.class,
				created.getIdElement().getIdPart(), created.getIdElement().getVersionIdPart());
		assertNotNull(read1);
		assertNotNull(read1.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), read1.getIdElement().getIdPart());
		assertNotNull(read1.getIdElement().getVersionIdPart());
		assertEquals("1", read1.getIdElement().getVersionIdPart());

		Bundle searchResult1a = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult1a);
		assertEquals(1, searchResult1a.getTotal());
		assertEquals(1, searchResult1a.getEntry().size());

		Bundle searchResult1b = getWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult1b);
		assertEquals(1, searchResult1b.getTotal());
		assertEquals(1, searchResult1b.getEntry().size());

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		created.setAuthored(new Date());
		created.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);

		QuestionnaireResponse updated = getMinimalWebserviceClient().update(created);
		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), updated.getIdElement().getIdPart());
		assertNotNull(updated.getIdElement().getVersionIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		QuestionnaireResponse read2 = getMinimalWebserviceClient().read(QuestionnaireResponse.class,
				updated.getIdElement().getIdPart(), updated.getIdElement().getVersionIdPart());
		assertNotNull(read2);
		assertNotNull(read2.getIdElement().getIdPart());
		assertEquals(updated.getIdElement().getIdPart(), read2.getIdElement().getIdPart());
		assertNotNull(read2.getIdElement().getVersionIdPart());
		assertEquals("2", read2.getIdElement().getVersionIdPart());

		Bundle searchResult2a = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult2a);
		assertEquals(1, searchResult2a.getTotal());
		assertEquals(1, searchResult2a.getEntry().size());

		Bundle searchResult2b = getAdminWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("_id", List.of(created.getIdElement().getIdPart())));
		assertNotNull(searchResult2b);
		assertEquals(1, searchResult2b.getTotal());
		assertEquals(1, searchResult2b.getEntry().size());
	}

	@Test
	public void testUpdateCompletedAmendedAllowedOrganizationUser() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse qr = createQuestionnaireResponse();
		qr.setStatus(QuestionnaireResponseStatus.COMPLETED);
		qr.setAuthored(new Date());
		qr.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(qr);

		created.setStatus(QuestionnaireResponseStatus.AMENDED);
		QuestionnaireResponse updated = getWebserviceClient().update(created);
		assertNotNull(updated);
	}

	@Test
	public void testUpdateCompletedAmendedAllowedDsfAdmin() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse qr = createQuestionnaireResponse();
		qr.setStatus(QuestionnaireResponseStatus.COMPLETED);
		qr.setAuthored(new Date());
		qr.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(qr);

		created.setStatus(QuestionnaireResponseStatus.AMENDED);
		QuestionnaireResponse updated = getAdminWebserviceClient().update(created);
		assertNotNull(updated);
	}

	@Test
	public void testUpdateCompletedAmendedNotAllowed() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse qr = createQuestionnaireResponse();
		qr.setStatus(QuestionnaireResponseStatus.COMPLETED);
		qr.setAuthored(new Date());
		qr.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(qr);

		created.setStatus(QuestionnaireResponseStatus.AMENDED);
		expectForbidden(() -> getMinimalWebserviceClient().update(created));
		expectForbidden(() -> getPractitionerWebserviceClient().update(created));
		expectForbidden(() -> getExternalWebserviceClient().update(created));
	}

	@Test
	public void testSearchAllowedByMinimalUserWithRole() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		Extension authExtension = questionnaireResponse.addExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization");
		authExtension.addExtension().setUrl("practitioner-role")
				.setValue(new Coding("http://dsf.dev/fhir/CodeSystem/practitioner-role", "DIC_USER", null));

		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		QuestionnaireResponse created = questionnaireResponseDao.create(questionnaireResponse);

		created.setStatus(QuestionnaireResponseStatus.COMPLETED);
		created.setAuthored(new Date());
		created.setAuthor(null).getAuthor().setType(ResourceType.Practitioner.name()).getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);

		QuestionnaireResponse updated = getMinimalWebserviceClient().update(created);
		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertEquals(created.getIdElement().getIdPart(), updated.getIdElement().getIdPart());
		assertNotNull(updated.getIdElement().getVersionIdPart());
		assertEquals("2", updated.getIdElement().getVersionIdPart());

		Bundle searchResult = getMinimalWebserviceClient().search(QuestionnaireResponse.class,
				Map.of("author:identifier",
						List.of("http://dsf.dev/sid/practitioner-identifier|" + X509Certificates.MINIMAL_CLIENT_MAIL)));
		assertNotNull(searchResult);
		assertEquals(1, searchResult.getTotal());
		assertNotNull(searchResult.getEntry());
		assertEquals(1, searchResult.getEntry().size());
		BundleEntryComponent entry = searchResult.getEntry().get(0);
		assertNotNull(entry);
		assertNotNull(entry.getResource());
		assertEquals(QuestionnaireResponse.class, entry.getResource().getClass());
		assertEquals(updated.getIdElement().getIdPart(), entry.getResource().getIdElement().getIdPart());
	}
}