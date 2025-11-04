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

import org.hl7.fhir.r4.model.HealthcareService;

import dev.dsf.fhir.dao.jdbc.HealthcareServiceDaoJdbc;

public class HealthcareServiceDaoTest extends AbstractReadAccessDaoTest<HealthcareService, HealthcareServiceDao>
{
	private static final String name = "Demo Healthcare Service";
	private static final boolean appointmentRequired = true;

	public HealthcareServiceDaoTest()
	{
		super(HealthcareService.class, HealthcareServiceDaoJdbc::new);
	}

	@Override
	public HealthcareService createResource()
	{
		HealthcareService healthcareService = new HealthcareService();
		healthcareService.setName(name);
		return healthcareService;
	}

	@Override
	protected void checkCreated(HealthcareService resource)
	{
		assertEquals(name, resource.getName());
	}

	@Override
	protected HealthcareService updateResource(HealthcareService resource)
	{
		resource.setAppointmentRequired(true);
		return resource;
	}

	@Override
	protected void checkUpdates(HealthcareService resource)
	{
		assertEquals(appointmentRequired, resource.getAppointmentRequired());
	}
}
