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

import static org.hl7.fhir.r4.model.Enumerations.PublicationStatus.ACTIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.util.Optional;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.NamingSystem.NamingSystemIdentifierType;
import org.junit.Test;

import dev.dsf.fhir.dao.jdbc.NamingSystemDaoJdbc;

public class NamingSystemDaoTest extends AbstractReadAccessDaoTest<NamingSystem, NamingSystemDao>
{
	private static final String name = "Demo NamingSystem Name";
	private static final String description = "Demo NamingSystem Description";
	private static final String uniqueIdValue = "http://foo.bar/sid/test";

	public NamingSystemDaoTest()
	{
		super(NamingSystem.class, NamingSystemDaoJdbc::new);
	}

	@Override
	public NamingSystem createResource()
	{
		NamingSystem namingSystem = new NamingSystem();
		namingSystem.setName(name);
		return namingSystem;
	}

	@Override
	protected void checkCreated(NamingSystem resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected NamingSystem updateResource(NamingSystem resource)
	{
		resource.setDescription(description);
		return resource;
	}

	@Override
	protected void checkUpdates(NamingSystem resource)
	{
		assertEquals(description, resource.getDescription());
	}

	@Test
	public void testReadByName() throws Exception
	{
		NamingSystem newResource = createResource();
		dao.create(newResource);

		Optional<NamingSystem> readByName = dao.readByName(name);
		assertTrue(readByName.isPresent());
	}

	@Test
	public void testExistsWithUniqueIdUriEntry() throws Exception
	{
		NamingSystem newResource = createResource();
		newResource.setStatus(ACTIVE);
		newResource.addUniqueId().setValue(uniqueIdValue).setType(NamingSystemIdentifierType.URI).addModifierExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference")
				.setValue(new BooleanType(true));
		dao.create(newResource);

		try (Connection connection = defaultDataSource.getConnection())
		{
			assertTrue(dao.existsWithUniqueIdUriEntry(connection, uniqueIdValue));
		}
	}

	@Test
	public void testExistsWithUniqueIdUriEntryTwoEntries() throws Exception
	{
		NamingSystem newResource = createResource();
		newResource.setStatus(ACTIVE);
		newResource.addUniqueId().setValue(uniqueIdValue).setType(NamingSystemIdentifierType.URI).addModifierExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference")
				.setValue(new BooleanType(true));
		newResource.addUniqueId().setValue(uniqueIdValue + "foo").setType(NamingSystemIdentifierType.URI);
		dao.create(newResource);

		try (Connection connection = defaultDataSource.getConnection())
		{
			assertTrue(dao.existsWithUniqueIdUriEntry(connection, uniqueIdValue));
		}
	}

	@Test
	public void testExistsWithUniqueIdUriEntryNotExisting() throws Exception
	{
		try (Connection connection = defaultDataSource.getConnection())
		{
			assertFalse(dao.existsWithUniqueIdUriEntry(connection, uniqueIdValue));
		}
	}

	@Test
	public void testExistsWithUniqueIdUriEntryResolvable() throws Exception
	{
		NamingSystem newResource = createResource();
		newResource.setStatus(ACTIVE);
		newResource.addUniqueId().setValue(uniqueIdValue).setType(NamingSystemIdentifierType.URI).addModifierExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference")
				.setValue(new BooleanType(true));
		dao.create(newResource);

		try (Connection connection = defaultDataSource.getConnection())
		{
			assertTrue(dao.existsWithUniqueIdUriEntryResolvable(connection, uniqueIdValue));
		}
	}

	@Test
	public void testExistsWithUniqueIdUriEntryResolvableSecondUniquIdNotResolvable() throws Exception
	{
		NamingSystem newResource = createResource();
		newResource.setStatus(ACTIVE);
		newResource.addUniqueId().setValue(uniqueIdValue).setType(NamingSystemIdentifierType.URI).addModifierExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference")
				.setValue(new BooleanType(true));
		newResource.addUniqueId().setValue(uniqueIdValue + "foo").setType(NamingSystemIdentifierType.URI)
				.addModifierExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference")
				.setValue(new BooleanType(false));
		dao.create(newResource);

		try (Connection connection = defaultDataSource.getConnection())
		{
			assertTrue(dao.existsWithUniqueIdUriEntryResolvable(connection, uniqueIdValue));
		}
	}

	@Test
	public void testExistsWithUniqueIdUriEntryResolvableSecondUniquIdNotResolvableNoExtension() throws Exception
	{
		NamingSystem newResource = createResource();
		newResource.setStatus(ACTIVE);
		newResource.addUniqueId().setValue(uniqueIdValue).setType(NamingSystemIdentifierType.URI).addModifierExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference")
				.setValue(new BooleanType(true));
		newResource.addUniqueId().setValue(uniqueIdValue + "foo").setType(NamingSystemIdentifierType.URI);
		dao.create(newResource);

		try (Connection connection = defaultDataSource.getConnection())
		{
			assertTrue(dao.existsWithUniqueIdUriEntryResolvable(connection, uniqueIdValue));
		}
	}

	@Test
	public void testExistsWithUniqueIdUriEntryResolvableWithoutUniqueId() throws Exception
	{
		NamingSystem newResource = createResource();
		newResource.setStatus(ACTIVE);
		dao.create(newResource);

		try (Connection connection = defaultDataSource.getConnection())
		{
			assertFalse(dao.existsWithUniqueIdUriEntryResolvable(connection, uniqueIdValue));
		}
	}

	@Test
	public void testExistsWithUniqueIdUriEntryResolvableWithoutModifierExtension() throws Exception
	{
		NamingSystem newResource = createResource();
		newResource.setStatus(ACTIVE);
		newResource.addUniqueId().setValue(uniqueIdValue).setType(NamingSystemIdentifierType.URI);
		dao.create(newResource);

		try (Connection connection = defaultDataSource.getConnection())
		{
			assertFalse(dao.existsWithUniqueIdUriEntryResolvable(connection, uniqueIdValue));
		}
	}

	@Test
	public void testExistsWithUniqueIdUriEntryResolvableWithModifierExtensionOfValueFalse() throws Exception
	{
		NamingSystem newResource = createResource();
		newResource.setStatus(ACTIVE);
		newResource.addUniqueId().setValue(uniqueIdValue).setType(NamingSystemIdentifierType.URI).addModifierExtension()
				.setUrl("http://dsf.dev/fhir/StructureDefinition/extension-check-logical-reference")
				.setValue(new BooleanType(false));
		dao.create(newResource);

		try (Connection connection = defaultDataSource.getConnection())
		{
			assertFalse(dao.existsWithUniqueIdUriEntryResolvable(connection, uniqueIdValue));
		}
	}
}
