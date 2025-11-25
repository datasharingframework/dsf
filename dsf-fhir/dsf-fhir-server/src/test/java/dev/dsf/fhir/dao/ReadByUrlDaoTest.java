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

import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.hl7.fhir.r4.model.DomainResource;
import org.junit.Test;

public interface ReadByUrlDaoTest<D extends DomainResource>
{
	D createResourceWithUrlAndVersion();

	String getUrl();

	String getVersion();

	ReadByUrlDao<D> readByUrlDao();

	ResourceDao<D> getDao();

	@Test
	default void testReadByUrlAndVersionWithUrl1() throws Exception
	{
		D newResource = createResourceWithUrlAndVersion();
		getDao().create(newResource);

		Optional<D> readByUrlAndVersion = readByUrlDao().readByUrlAndVersion(getUrl());
		assertTrue(readByUrlAndVersion.isPresent());
	}

	@Test
	default void testReadByUrlAndVersionWithUrlAndVersion1() throws Exception
	{
		D newResource = createResourceWithUrlAndVersion();
		getDao().create(newResource);

		Optional<D> readByUrlAndVersion = readByUrlDao().readByUrlAndVersion(getUrl() + "|" + getVersion());
		assertTrue(readByUrlAndVersion.isPresent());
	}

	@Test
	default void testReadByUrlAndVersionWithUrl2() throws Exception
	{
		D newResource = createResourceWithUrlAndVersion();
		getDao().create(newResource);

		Optional<D> readByUrlAndVersion = readByUrlDao().readByUrlAndVersion(getUrl(), null);
		assertTrue(readByUrlAndVersion.isPresent());
	}

	@Test
	default void testReadByUrlAndVersionWithUrlAndVersion2() throws Exception
	{
		D newResource = createResourceWithUrlAndVersion();
		getDao().create(newResource);

		Optional<D> readByUrlAndVersion = readByUrlDao().readByUrlAndVersion(getUrl(), getVersion());
		assertTrue(readByUrlAndVersion.isPresent());
	}
}
