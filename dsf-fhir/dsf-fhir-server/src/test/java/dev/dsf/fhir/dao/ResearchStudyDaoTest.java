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

import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.ResearchStudy;

import dev.dsf.fhir.dao.jdbc.ResearchStudyDaoJdbc;

public class ResearchStudyDaoTest extends AbstractReadAccessDaoTest<ResearchStudy, ResearchStudyDao>
{
	private static final String title = "Demo Research Study";
	private final Date periodStart = new GregorianCalendar(2019, 0, 1).getTime();
	private final Date periodEnd = new GregorianCalendar(2021, 11, 31).getTime();

	public ResearchStudyDaoTest()
	{
		super(ResearchStudy.class, ResearchStudyDaoJdbc::new);
	}

	@Override
	public ResearchStudy createResource()
	{
		ResearchStudy researchStudy = new ResearchStudy();
		researchStudy.setTitle(title);
		return researchStudy;
	}

	@Override
	protected void checkCreated(ResearchStudy resource)
	{
		assertEquals(title, resource.getTitle());
	}

	@Override
	protected ResearchStudy updateResource(ResearchStudy resource)
	{
		resource.setPeriod(new Period().setStart(periodStart).setEnd(periodEnd));
		return resource;
	}

	@Override
	protected void checkUpdates(ResearchStudy resource)
	{
		assertEquals(periodStart, resource.getPeriod().getStart());
		assertEquals(periodEnd, resource.getPeriod().getEnd());
	}
}
