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
package dev.dsf.fhir.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;
import org.junit.Test;

import dev.dsf.fhir.dao.jdbc.SubscriptionDaoJdbc;

public class SubscriptionDaoTest extends AbstractReadAccessDaoTest<Subscription, SubscriptionDao>
{
	private static final String reason = "Demo Subscription Reason";
	private static final SubscriptionStatus status = SubscriptionStatus.ACTIVE;

	public SubscriptionDaoTest()
	{
		super(Subscription.class, SubscriptionDaoJdbc::new);
	}

	@Override
	public Subscription createResource()
	{
		Subscription subscription = new Subscription();
		subscription.setStatus(status);
		return subscription;
	}

	@Override
	protected void checkCreated(Subscription resource)
	{
		assertEquals(status, resource.getStatus());
	}

	@Override
	protected Subscription updateResource(Subscription resource)
	{
		resource.setReason(reason);
		return resource;
	}

	@Override
	protected void checkUpdates(Subscription resource)
	{
		assertEquals(reason, resource.getReason());
	}

	@Test
	public void testExistsActiveNotDeletedByAddressDeleted() throws Exception
	{
		Subscription activeSubscriptionToDelete = createResource();
		Subscription createdActiveSubscriptionToDelete = dao.create(activeSubscriptionToDelete);
		assertNotNull(createdActiveSubscriptionToDelete);

		boolean deleted = dao.delete(UUID.fromString(createdActiveSubscriptionToDelete.getIdElement().getIdPart()));
		assertTrue(deleted);

		Subscription activeSubscription = createResource();
		Subscription createdActiveSubscription = dao.create(activeSubscription);
		assertNotNull(createdActiveSubscription);

		Subscription offSubscription = createResource();
		offSubscription.setStatus(SubscriptionStatus.OFF);
		Subscription createdOffSubscription = dao.create(offSubscription);
		assertNotNull(createdOffSubscription);

		List<Subscription> activeSubscriptions = dao.readByStatus(SubscriptionStatus.ACTIVE);
		assertNotNull(activeSubscriptions);
		assertEquals(1, activeSubscriptions.size());
	}
}
