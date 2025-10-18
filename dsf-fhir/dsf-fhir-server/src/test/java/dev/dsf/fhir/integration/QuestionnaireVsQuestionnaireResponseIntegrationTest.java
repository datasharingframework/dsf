package dev.dsf.fhir.integration;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Questionnaire;
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
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.0.0");
		Questionnaire questionnaire2 = createQuestionnaireProfileVersion150("2.0.0");
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);
		questionnaireDao.create(questionnaire2);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.0.0");
		QuestionnaireResponse created = getWebserviceClient().create(questionnaireResponse);

		created.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		String newQuestionnaireUrlAndVersion = created.getQuestionnaire().replace("1.0.0", "2.0.0");
		created.setQuestionnaire(newQuestionnaireUrlAndVersion);

		expectForbidden(() -> getWebserviceClient().update(created));
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireProfilVersion100QuestionnaireNotFound()
			throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.0.1");
		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("2.0.1");
		expectForbidden(() -> getWebserviceClient().create(questionnaireResponse));
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireProfilVersion100ItemSet() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.0.2");
		addItem(questionnaire, "test", "test-item", Questionnaire.QuestionnaireItemType.STRING, Optional.empty());

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.0.2");
		QuestionnaireResponse createdQr = getWebserviceClient().create(questionnaireResponse);

		createdQr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		addItem(createdQr, "test", "test-item", new StringType("answer"));
		QuestionnaireResponse updatedQr = getWebserviceClient().update(createdQr);

		assertNotNull(updatedQr);
		assertNotNull(updatedQr.getIdElement().getIdPart());
		assertNotNull(updatedQr.getIdElement().getVersionIdPart());
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireProfilVersion100ItemMissing() throws Exception
	{
		// expected to work without validation error, since not setting Questionnaire.item.required means the item is
		// not required
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.0.3");
		addItem(questionnaire, "test", "test-item", Questionnaire.QuestionnaireItemType.STRING, Optional.empty());

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.0.3");
		QuestionnaireResponse createdQr = getWebserviceClient().create(questionnaireResponse);

		createdQr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		QuestionnaireResponse updatedQr = getWebserviceClient().update(createdQr);

		assertNotNull(updatedQr);
		assertNotNull(updatedQr.getIdElement().getIdPart());
		assertNotNull(updatedQr.getIdElement().getVersionIdPart());
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireProfileVersion150ItemRequiredAndSet()
			throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.5.1");
		addItem(questionnaire, "test", "test-item", Questionnaire.QuestionnaireItemType.STRING, Optional.of(true));

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.5.1");
		QuestionnaireResponse createdQr = getWebserviceClient().create(questionnaireResponse);

		createdQr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		addItem(createdQr, "test", "test-item", new StringType("answer"));
		QuestionnaireResponse updatedQr = getWebserviceClient().update(createdQr);

		assertNotNull(updatedQr);
		assertNotNull(updatedQr.getIdElement().getIdPart());
		assertNotNull(updatedQr.getIdElement().getVersionIdPart());
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireProfileVersion150ItemRequiredNotSet()
			throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.5.2");
		addItem(questionnaire, "test", "test-item", Questionnaire.QuestionnaireItemType.STRING, Optional.of(true));

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.5.2");
		QuestionnaireResponse createdQr = getWebserviceClient().create(questionnaireResponse);

		createdQr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
		expectForbidden(() -> getWebserviceClient().update(createdQr));
	}

	@Test
	public void testQuestionnaireResponseValidatesAgainstQuestionnaireProfileVersion150ItemOptionalNotSet()
			throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.5.3");
		addItem(questionnaire, "test", "test-item", Questionnaire.QuestionnaireItemType.STRING, Optional.of(false));

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.5.3");
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
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.5.3");
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.5.3");

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
		Questionnaire questionnaire = createQuestionnaireProfileVersion150("1.5.3");
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.5.3");

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
		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.5.3");

		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.TRANSACTION);
		bundle.addEntry().setResource(questionnaireResponse).setFullUrl("urn:uuid:" + UUID.randomUUID().toString())
				.getRequest().setMethod(Bundle.HTTPVerb.POST).setUrl(ResourceType.QuestionnaireResponse.name());

		expectForbidden(() -> getWebserviceClient().postBundle(bundle));
	}

	@Test
	public void testQuestionnaireResponseQuestionnaireDisplayItemChangedWithMinimalUser() throws Exception
	{
		Questionnaire questionnaire = createQuestionnaireProfileVersion100("1.0.0");
		questionnaire.addItem().setLinkId("display-id").setType(Questionnaire.QuestionnaireItemType.DISPLAY)
				.setText("Default Text Value");

		QuestionnaireDao questionnaireDao = getSpringWebApplicationContext().getBean(QuestionnaireDao.class);
		questionnaireDao.create(questionnaire);

		QuestionnaireResponse questionnaireResponse = createQuestionnaireResponse("1.0.0");
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