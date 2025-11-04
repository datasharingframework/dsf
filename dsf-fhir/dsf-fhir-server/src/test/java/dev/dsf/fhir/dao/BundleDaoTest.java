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

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;

import dev.dsf.fhir.dao.jdbc.BundleDaoJdbc;

public class BundleDaoTest extends AbstractReadAccessDaoTest<Bundle, BundleDao>
{
	private static final BundleType type = BundleType.SEARCHSET;
	private static final String language = "Demo Bundle language";

	public BundleDaoTest()
	{
		super(Bundle.class, BundleDaoJdbc::new);
	}

	@Override
	public Bundle createResource()
	{
		Bundle bundle = new Bundle();
		bundle.setType(type);
		return bundle;
	}

	@Override
	protected void checkCreated(Bundle resource)
	{
		assertEquals(type, resource.getType());
	}

	@Override
	protected Bundle updateResource(Bundle resource)
	{
		resource.setLanguage(language);
		return resource;
	}

	@Override
	protected void checkUpdates(Bundle resource)
	{
		assertEquals(language, resource.getLanguage());
	}
}
