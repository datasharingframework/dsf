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

import java.util.Date;

import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.MeasureReport;
import org.junit.Test;

import dev.dsf.fhir.dao.MeasureDao;

public class MeasureReportIntegrationTest extends AbstractIntegrationTest
{
	private static Measure createMeasure()
	{
		Measure measure = new Measure();
		measure.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		measure.setUrl("https://foo.bar/fhir/Measure/8cc30173-8b85-4418-882a-a3b8a9652fc6");
		measure.setStatus(Enumerations.PublicationStatus.ACTIVE);
		measure.getScoring().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/measure-scoring")
				.setCode("cohort");

		Measure.MeasureGroupPopulationComponent population = measure.getGroupFirstRep().getPopulationFirstRep();
		population.getCode().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
				.setCode("initial-population");
		population.getCriteria().setLanguage("text/cql").setExpression("InInitialPopulation");

		return measure;
	}

	@Test
	public void testCreateValidByLocalUser() throws Exception
	{
		MeasureDao measureDao = getSpringWebApplicationContext().getBean(MeasureDao.class);
		Measure measure = measureDao.create(createMeasure());
		assertEquals("https://foo.bar/fhir/Measure/8cc30173-8b85-4418-882a-a3b8a9652fc6", measure.getUrl());

		MeasureReport measureReport = new MeasureReport();
		measureReport.getMeta().addTag().setSystem("http://dsf.dev/fhir/CodeSystem/read-access-tag").setCode("ALL");
		measureReport.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
		measureReport.setType(MeasureReport.MeasureReportType.SUMMARY);
		measureReport.setMeasure("https://foo.bar/fhir/Measure/8cc30173-8b85-4418-882a-a3b8a9652fc6");
		measureReport.getPeriod().setStart(new Date());

		MeasureReport.MeasureReportGroupPopulationComponent population = measureReport.getGroupFirstRep()
				.getPopulationFirstRep();
		population.getCode().addCoding().setSystem("http://terminology.hl7.org/CodeSystem/measure-population")
				.setCode("initial-population");
		population.setCount(42);

		MeasureReport created = getWebserviceClient().create(measureReport);
		assertNotNull(created);
		assertNotNull(created.getIdElement().getIdPart());
		assertNotNull(created.getIdElement().getVersionIdPart());
	}
}
