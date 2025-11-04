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

import java.util.Date;
import java.util.GregorianCalendar;

import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.fhir.dao.jdbc.PractitionerDaoJdbc;

public class PractitionerDaoTest extends AbstractReadAccessDaoTest<Practitioner, PractitionerDao>
{
	private final Date birthday = new GregorianCalendar(1980, 0, 2).getTime();
	private final AdministrativeGender gender = AdministrativeGender.FEMALE;

	public PractitionerDaoTest()
	{
		super(Practitioner.class, PractitionerDaoJdbc::new);
	}

	@Override
	public Practitioner createResource()
	{
		Practitioner practitioner = new Practitioner();
		practitioner.setBirthDate(birthday);
		return practitioner;
	}

	@Override
	protected void checkCreated(Practitioner resource)
	{
		assertEquals(birthday, resource.getBirthDate());
	}

	@Override
	protected Practitioner updateResource(Practitioner resource)
	{
		resource.setGender(gender);
		return resource;
	}

	@Override
	protected void checkUpdates(Practitioner resource)
	{
		assertEquals(gender, resource.getGender());
	}
}
