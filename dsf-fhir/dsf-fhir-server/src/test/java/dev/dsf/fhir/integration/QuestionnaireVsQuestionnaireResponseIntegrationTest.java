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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.Questionnaire.QuestionnaireItemType;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

import dev.dsf.fhir.dao.QuestionnaireDao;

public class QuestionnaireVsQuestionnaireResponseIntegrationTest extends AbstractQuestionnaireIntegrationTest
{
	@Test
	public void testQuestionnaireResponseQuestionnaireCanonicalChanged() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponse created = getWebserviceClient().create(questionnaireResponse);

		created.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		String newQuestionnaireUrlAndVersion = created.getQuestionnaire().replace(QUESTIONNAIRE_VERSION, "2.1");
		created.setQuestionnaire(newQuestionnaireUrlAndVersion);

		expectForbidden(() -> getWebserviceClient().update(created));
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireItemRequiredAndSet() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		questionnaire.addItem().setLinkId("test").setText("test-item").setType(QuestionnaireItemType.STRING)
				.setRequired(true);

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponse createdQr = getWebserviceClient().create(questionnaireResponse);

		createdQr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		addItem(createdQr, "test", "test-item", new StringType("answer"));
		QuestionnaireResponse updatedQr = getWebserviceClient().update(createdQr);

		assertNotNull(updatedQr);
		assertNotNull(updatedQr.getIdElement().getIdPart());
		assertNotNull(updatedQr.getIdElement().getVersionIdPart());
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireItemRequiredNotSet() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		questionnaire.addItem().setLinkId("test").setText("test-item").setType(QuestionnaireItemType.STRING)
				.setRequired(true);

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponse createdQr = getWebserviceClient().create(questionnaireResponse);

		createdQr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		expectForbidden(() -> getWebserviceClient().update(createdQr));
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireItemOptionalNotSet() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		questionnaire.addItem().setLinkId("test").setText("test-item").setType(QuestionnaireItemType.STRING)
				.setRequired(false);

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		QuestionnaireResponse createdQr = getWebserviceClient().create(questionnaireResponse);

		createdQr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		QuestionnaireResponse updatedQr = getWebserviceClient().update(createdQr);

		assertNotNull(updatedQr);
		assertNotNull(updatedQr.getIdElement().getIdPart());
		assertNotNull(updatedQr.getIdElement().getVersionIdPart());
	}

	@Test
	public void testPostQuestionnaireAndCorrespondingQuestionnaireResponseInTransactionBundleOrderQuestionnaireBeforeQuestionnaireResponse()
			throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();

		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.TRANSACTION);
		bundle.addEntry().setResource(questionnaire).setFullUrl("urn:uuid:" + UUID.randomUUID().toString()).getRequest()
				.setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.Questionnaire.name());
		bundle.addEntry().setResource(questionnaireResponse).setFullUrl("urn:uuid:" + UUID.randomUUID().toString())
				.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.QuestionnaireResponse.name());

		assertTrue(getWebserviceClient().postBundle(bundle).getEntry().stream()
				.allMatch(entry -> entry.getResponse().getStatus().equals("201 Created")));
	}

	@Test
	public void testPostQuestionnaireAndCorrespondingQuestionnaireResponseInTransactionBundleOrderQuestionnaireResponseBeforeQuestionnaire()
			throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();

		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.TRANSACTION);
		bundle.addEntry().setResource(questionnaireResponse).setFullUrl("urn:uuid:" + UUID.randomUUID().toString())
				.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.QuestionnaireResponse.name());
		bundle.addEntry().setResource(questionnaire).setFullUrl("urn:uuid:" + UUID.randomUUID().toString()).getRequest()
				.setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.Questionnaire.name());

		assertTrue(getWebserviceClient().postBundle(bundle).getEntry().stream()
				.allMatch(entry -> entry.getResponse().getStatus().equals("201 Created")));
	}

	@Test
	public void testPostQuestionnaireResponseInTransactionBundleQuestionnaireDoesNotExistForbidden() throws Exception
	{
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();

		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.TRANSACTION);
		bundle.addEntry().setResource(questionnaireResponse).setFullUrl("urn:uuid:" + UUID.randomUUID().toString())
				.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.QuestionnaireResponse.name());

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testQuestionnaireResponseQuestionnaireDisplayItemChangedWithMinimalUser() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaire();
		questionnaire.addItem().setLinkId("display-id").setType(QuestionnaireItemType.DISPLAY)
				.setText("Default Text Value");

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse();
		questionnaireResponse.addItem().setLinkId("display-id").setText("Default Text Value");

		expectForbidden(() -> getMinimalWebserviceClient().create(questionnaireResponse));

		QuestionnaireResponse created = getWebserviceClient().create(questionnaireResponse);

		created.getItem().stream().filter(i -> "display-id".equals(i.getLinkId())).findFirst()
				.ifPresent(i -> i.setText("Response Test Value"));
		created.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		created.setAuthored(new Date());
		created.getAuthor().setType("Practitioner").getIdentifier()
				.setSystem("http://dsf.dev/sid/practitioner-identifier").setValue(X509Certificates.MINIMAL_CLIENT_MAIL);

		QuestionnaireResponse updated = getMinimalWebserviceClient().update(created);

		assertNotNull(updated);
		assertNotNull(updated.getIdElement().getIdPart());
		assertNotNull(updated.getIdElement().getVersionIdPart());
	}
}