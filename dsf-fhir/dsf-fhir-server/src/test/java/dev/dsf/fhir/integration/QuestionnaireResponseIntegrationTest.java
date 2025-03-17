package dev.dsf.fhir.integration;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.QuestionnaireResponse.QuestionnaireResponseStatus;
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
		Questionnaire questionnaire = createQuestionnaireProfileVersion100();
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
		Questionnaire questionnaire = createQuestionnaireProfileVersion100();
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
		Questionnaire questionnaire = createQuestionnaireProfileVersion100();
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
		Questionnaire questionnaire = createQuestionnaireProfileVersion100().setVersion(null);
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

		Questionnaire questionnaire1 = createQuestionnaireProfileVersion100().setVersion("0.1.0");
		questionnaireDao.create(questionnaire1);

		Questionnaire questionnaire2 = createQuestionnaireProfileVersion100();
		questionnaireDao.create(questionnaire2);

		Questionnaire questionnaire3 = createQuestionnaireProfileVersion100().setVersion("0.2.0");
		questionnaireDao.create(questionnaire3);

		Questionnaire questionnaire4 = createQuestionnaireProfileVersion100().setVersion(null);
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
				Map.of("status", List.of(QUESTIONNAIRE_RESPONSE_STATUS.toCode())));

		assertNotNull(searchBundle.getEntry());
		assertEquals(1, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse searchQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(searchQuestionnaireResponse.hasStatus());
		assertEquals(QUESTIONNAIRE_RESPONSE_STATUS, searchQuestionnaireResponse.getStatus());
	}

	@Test
	public void testSearchBySubjectReference() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponseDao questionnaireResponseDao = getSpringWebApplicationContext()
				.getBean(QuestionnaireResponseDao.class);
		questionnaireResponseDao.create(questionnaireResponse);

		OrganizationProvider organizationProvider = getSpringWebApplicationContext()
				.getBean(OrganizationProvider.class);
		Organization localOrganization = organizationProvider.getLocalOrganization().get();
		String organizationReference = "Organization/" + localOrganization.getIdElement().getIdPart();

		Bundle searchBundle = getWebserviceClient().search(QuestionnaireResponse.class, Map.of("subject",
				List.of(organizationReference), "_include", List.of("QuestionnaireResponse:subject:Organization")));

		assertNotNull(searchBundle.getEntry());
		assertEquals(2, searchBundle.getEntry().size());
		assertNotNull(searchBundle.getEntry().get(0));
		assertNotNull(searchBundle.getEntry().get(0).getResource());
		assertTrue(searchBundle.getEntry().get(0).getResource() instanceof QuestionnaireResponse);

		QuestionnaireResponse searchQuestionnaireResponse = (QuestionnaireResponse) searchBundle.getEntry().get(0)
				.getResource();
		assertTrue(searchQuestionnaireResponse.hasStatus());
		assertEquals(organizationReference, searchQuestionnaireResponse.getSubject().getReference());

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
}