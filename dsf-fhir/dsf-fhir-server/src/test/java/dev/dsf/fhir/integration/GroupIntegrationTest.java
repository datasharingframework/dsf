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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupType;
import org.junit.Test;

import dev.dsf.fhir.dao.GroupDao;

public class GroupIntegrationTest extends AbstractIntegrationTest
{
	@Test
	public void testCreateGroup() throws Exception
	{
		Group g = createGroup();
		getReadAccessHelper().addAll(g);

		getWebserviceClient().create(g);
	}

	@Test
	public void testCreateGroupFrobidden() throws Exception
	{
		expectForbidden(() -> getWebserviceClient().create(createGroup()));
	}

	@Test
	public void testSearchGroupByIdentifier() throws Exception
	{
		final String identifier = UUID.randomUUID().toString();

		Group g = createGroup(identifier);
		getReadAccessHelper().addAll(g);

		GroupDao dao = getSpringWebApplicationContext().getBean(GroupDao.class);
		Group created = dao.create(g);
		assertNotNull(created);
		assertTrue(created.hasIdElement());
		assertTrue(created.hasIdentifier());
		assertEquals(identifier, created.getIdentifierFirstRep().getValue());

		Bundle bundle = getWebserviceClient().search(Group.class, Map.of("identifier", List.of(identifier)));
		assertNotNull(bundle);
		assertEquals(1, bundle.getTotal());
		assertTrue(bundle.hasEntry());
		assertEquals(1, bundle.getEntry().size());
		assertTrue(bundle.getEntry().get(0).hasResource());
		assertTrue(bundle.getEntry().get(0).getResource() instanceof Group);
		assertTrue(bundle.getEntry().get(0).getResource().hasIdElement());
		assertEquals(created.getIdElement(), bundle.getEntry().get(0).getResource().getIdElement());
	}

	@Test
	public void testSearchGroupByTwoIdentifiers() throws Exception
	{
		final String identifier2 = UUID.randomUUID().toString();
		final String identifier1 = UUID.randomUUID().toString();

		Group g1 = createGroup(identifier1, identifier2);
		getReadAccessHelper().addAll(g1);
		GroupDao dao = getSpringWebApplicationContext().getBean(GroupDao.class);
		Group created1 = dao.create(g1);
		assertNotNull(created1);
		assertTrue(created1.hasIdElement());

		Group g2 = createGroup(identifier1);
		getReadAccessHelper().addAll(g2);
		Group created2 = dao.create(g2);
		assertNotNull(created2);
		assertTrue(created2.hasIdElement());

		Bundle bundle = getWebserviceClient().search(Group.class,
				Map.of("identifier", List.of(identifier1, identifier2)));
		assertNotNull(bundle);
		assertEquals(1, bundle.getTotal());
		assertTrue(bundle.hasEntry());
		assertEquals(1, bundle.getEntry().size());
		assertTrue(bundle.getEntry().get(0).hasResource());
		assertTrue(bundle.getEntry().get(0).getResource() instanceof Group);
		assertTrue(bundle.getEntry().get(0).getResource().hasIdElement());
		assertEquals(created1.getIdElement(), bundle.getEntry().get(0).getResource().getIdElement());
	}

	private Group createGroup(String... identifiers)
	{
		Group g = new Group();
		g.setType(GroupType.PERSON);
		g.setActual(false);
		for (String identifier : identifiers)
			g.addIdentifier().setSystem("http://test.org/sid/group-id").setValue(identifier);

		return g;
	}
}
