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

import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.Test;

import dev.dsf.fhir.dao.jdbc.StructureDefinitionDaoJdbc;

public class StructureDefinitionDaoTest extends AbstractReadAccessDaoTest<StructureDefinition, StructureDefinitionDao>
		implements ReadByUrlDaoTest<StructureDefinition>
{
	private static final String name = "StructureDefinition";
	private static final String title = "Demo Structure Definition";

	public StructureDefinitionDaoTest()
	{
		super(StructureDefinition.class, StructureDefinitionDaoJdbc::new);
	}

	@Override
	public StructureDefinition createResource()
	{
		StructureDefinition structureDefinition = new StructureDefinition();
		structureDefinition.setName(name);
		return structureDefinition;
	}

	@Override
	protected void checkCreated(StructureDefinition resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected StructureDefinition updateResource(StructureDefinition resource)
	{
		resource.setTitle(title);
		return resource;
	}

	@Override
	protected void checkUpdates(StructureDefinition resource)
	{
		assertEquals(title, resource.getTitle());
	}

	@Override
	public StructureDefinition createResourceWithUrlAndVersion()
	{
		StructureDefinition resource = createResource();
		resource.setUrl(getUrl());
		resource.setVersion(getVersion());
		return resource;
	}

	@Override
	public String getUrl()
	{
		return "http://test.com/fhir/StructureDefinition/test-system";
	}

	@Override
	public String getVersion()
	{
		return "0.3.0";
	}

	@Override
	public ReadByUrlDao<StructureDefinition> readByUrlDao()
	{
		return getDao();
	}

	@Override
	@Test
	public void testReadByUrlAndVersionWithUrl1() throws Exception
	{
		ReadByUrlDaoTest.super.testReadByUrlAndVersionWithUrl1();
	}

	@Override
	@Test
	public void testReadByUrlAndVersionWithUrlAndVersion1() throws Exception
	{
		ReadByUrlDaoTest.super.testReadByUrlAndVersionWithUrlAndVersion1();
	}

	@Override
	@Test
	public void testReadByUrlAndVersionWithUrl2() throws Exception
	{
		ReadByUrlDaoTest.super.testReadByUrlAndVersionWithUrl2();
	}

	@Override
	@Test
	public void testReadByUrlAndVersionWithUrlAndVersion2() throws Exception
	{
		ReadByUrlDaoTest.super.testReadByUrlAndVersionWithUrlAndVersion2();
	}
}
