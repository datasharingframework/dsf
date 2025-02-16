package dev.dsf.fhir.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionChannelType;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.junit.Test;

import dev.dsf.fhir.dao.SubscriptionDao;

public class SubscriptionIntegrationTest extends AbstractIntegrationTest
{
	private Subscription newSubscription(String criteria)
	{
		Subscription s = new Subscription();
		s.setStatus(SubscriptionStatus.ACTIVE);
		s.getChannel().setType(SubscriptionChannelType.WEBSOCKET).setPayload("application/fhir+json");
		s.setCriteria(criteria);
		s.setReason("Integration-Test");

		readAccessHelper.addLocal(s);

		return s;
	}

	@Test
	public void testCreateOkJson() throws Exception
	{
		Subscription t = newSubscription("Task?status=completed");

		Subscription created = getWebserviceClient().create(t);
		assertNotNull(created);
		assertTrue(created.getIdElement().hasValue());
		assertEquals("1", created.getMeta().getVersionId());
	}

	@Test
	public void testCreateOkXml() throws Exception
	{
		Subscription t = newSubscription("Task?status=completed");
		t.getChannel().setPayload("application/fhir+xml");

		Subscription created = getWebserviceClient().create(t);
		assertNotNull(created);
		assertTrue(created.getIdElement().hasValue());
		assertEquals("1", created.getMeta().getVersionId());
	}

	@Test
	public void testCreateOkNoPayload() throws Exception
	{
		Subscription t = newSubscription("Task?status=completed");
		t.getChannel().setPayload(null);

		Subscription created = getWebserviceClient().create(t);
		assertNotNull(created);
		assertTrue(created.getIdElement().hasValue());
		assertEquals("1", created.getMeta().getVersionId());
	}

	@Test
	public void testCreateOkNoPayloadAllreadyExistsWithPayload() throws Exception
	{
		Subscription t = newSubscription("Task?status=completed");

		SubscriptionDao dao = getSpringWebApplicationContext().getBean(SubscriptionDao.class);
		dao.create(t);

		t = newSubscription("Task?status=completed");
		t.getChannel().setPayload(null);

		Subscription created = getWebserviceClient().create(t);
		assertNotNull(created);
		assertTrue(created.getIdElement().hasValue());
		assertEquals("1", created.getMeta().getVersionId());
	}

	@Test
	public void testCreateNotOkNoPayloadAllreadyExistsWithoutPayload() throws Exception
	{
		Subscription t = newSubscription("Task?status=completed");
		t.getChannel().setPayload(null);

		SubscriptionDao dao = getSpringWebApplicationContext().getBean(SubscriptionDao.class);
		dao.create(t);

		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateInvalid() throws Exception
	{
		Subscription noStatus = newSubscription("Task?status=completed");
		noStatus.setStatus(null);
		expectForbidden(() -> getWebserviceClient().create(noStatus));

		Subscription noChannel = newSubscription("Task?status=completed");
		noChannel.setChannel(null);
		expectForbidden(() -> getWebserviceClient().create(noChannel));

		Subscription noChannelType = newSubscription("Task?status=completed");
		noChannelType.getChannel().setType(null);
		expectForbidden(() -> getWebserviceClient().create(noChannelType));

		Subscription channelTypeEmail = newSubscription("Task?status=completed");
		channelTypeEmail.getChannel().setType(SubscriptionChannelType.EMAIL);
		expectForbidden(() -> getWebserviceClient().create(channelTypeEmail));

		// no payload allowed -> websockets sends "ping" (standard FHIR behavior for websockets)

		Subscription channelPayloadPdf = newSubscription("Task?status=completed");
		channelPayloadPdf.getChannel().setPayload("application/pdf");
		expectForbidden(() -> getWebserviceClient().create(channelPayloadPdf));

		Subscription noCriteria = newSubscription(null);
		expectForbidden(() -> getWebserviceClient().create(noCriteria));

		Subscription noReadAccessTag = newSubscription("Task?status=completed");
		noReadAccessTag.getMeta().setTag(null);
		expectForbidden(() -> getWebserviceClient().create(noReadAccessTag));
	}

	@Test
	public void testCreateExisting() throws Exception
	{
		Subscription t = newSubscription("Task?status=requested");
		expectForbidden(() -> getWebserviceClient().create(t));

		Subscription q = newSubscription("QuestionnaireResponse?status=completed");
		expectForbidden(() -> getWebserviceClient().create(q));
	}

	@Test
	public void testCreateUnsupportedSearchParameterInCriteria() throws Exception
	{
		Subscription t = newSubscription("Task?status=requested&unsupported=true");
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateUnsupportedResourcerInCriteria() throws Exception
	{
		Subscription t = newSubscription("CareTeam?status=active");
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testCreateUnsupportedResourcerInCriteriaPath() throws Exception
	{
		Subscription t = newSubscription("fhir/Task?status=completed");
		expectForbidden(() -> getWebserviceClient().create(t));
	}

	@Test
	public void testUpdateOk() throws Exception
	{
		Subscription t = newSubscription("Task?status=completed");
		SubscriptionDao dao = getSpringWebApplicationContext().getBean(SubscriptionDao.class);
		Subscription created = dao.create(t);
		assertNotNull(created);

		created.setReason("Update Test");
		Subscription updated = getWebserviceClient().update(created);
		assertNotNull(updated);
		assertTrue(updated.getIdElement().hasValue());
		assertEquals("2", updated.getMeta().getVersionId());
	}

	@Test
	public void testUpdateNotAllowed() throws Exception
	{
		Subscription t = newSubscription("Task?status=completed");
		SubscriptionDao dao = getSpringWebApplicationContext().getBean(SubscriptionDao.class);
		Subscription created = dao.create(t);
		assertNotNull(created);

		created.setCriteria("Task?status=failed");
		expectForbidden(() -> getWebserviceClient().update(created));

		created.setCriteria("Task?status=completed");
		created.getChannel().setPayload("application/fhir+xml");
		expectForbidden(() -> getWebserviceClient().update(created));

		created.getChannel().setPayload(null);
		expectForbidden(() -> getWebserviceClient().update(created));
	}
}
