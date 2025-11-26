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

import org.hl7.fhir.r4.model.Group;

import dev.dsf.fhir.dao.jdbc.GroupDaoJdbc;

public class GroupDaoTest extends AbstractReadAccessDaoTest<Group, GroupDao>
{
	private static final String name = "Demo Group";
	private static final Group.GroupType type = Group.GroupType.PERSON;
	private static final boolean actual = true;

	public GroupDaoTest()
	{
		super(Group.class, GroupDaoJdbc::new);
	}

	@Override
	public Group createResource()
	{
		Group group = new Group();
		group.setType(type);
		group.setActual(actual);
		return group;
	}

	@Override
	protected void checkCreated(Group resource)
	{
		assertEquals(type, resource.getType());
		assertEquals(actual, resource.getActual());
	}

	@Override
	protected Group updateResource(Group resource)
	{
		resource.setName(name);
		return resource;
	}

	@Override
	protected void checkUpdates(Group resource)
	{
		assertEquals(name, resource.getName());
	}
}
