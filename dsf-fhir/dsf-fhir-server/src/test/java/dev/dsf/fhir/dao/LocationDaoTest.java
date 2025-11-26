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

import org.hl7.fhir.r4.model.Location;

import dev.dsf.fhir.dao.jdbc.LocationDaoJdbc;

public class LocationDaoTest extends AbstractReadAccessDaoTest<Location, LocationDao>
{
	private static final String name = "Demo Location";
	private static final String description = "Demo Location Description";

	public LocationDaoTest()
	{
		super(Location.class, LocationDaoJdbc::new);
	}

	@Override
	public Location createResource()
	{
		Location location = new Location();
		location.setName(name);
		return location;
	}

	@Override
	protected void checkCreated(Location resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected Location updateResource(Location resource)
	{
		resource.setDescription(description);
		return resource;
	}

	@Override
	protected void checkUpdates(Location resource)
	{
		assertEquals(description, resource.getDescription());
	}
}
