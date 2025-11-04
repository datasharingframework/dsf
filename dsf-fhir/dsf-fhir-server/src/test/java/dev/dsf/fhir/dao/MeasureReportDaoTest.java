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

import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.MeasureReport.MeasureReportStatus;

import dev.dsf.fhir.dao.jdbc.MeasureReportDaoJdbc;

public class MeasureReportDaoTest extends AbstractReadAccessDaoTest<MeasureReport, MeasureReportDao>
{
	public MeasureReportDaoTest()
	{
		super(MeasureReport.class, MeasureReportDaoJdbc::new);
	}

	@Override
	public MeasureReport createResource()
	{
		MeasureReport measureReport = new MeasureReport();
		measureReport.setStatus(MeasureReportStatus.PENDING);
		return measureReport;
	}

	@Override
	protected void checkCreated(MeasureReport resource)
	{
		assertEquals(MeasureReportStatus.PENDING, resource.getStatus());
	}

	@Override
	protected MeasureReport updateResource(MeasureReport resource)
	{
		resource.setStatus(MeasureReportStatus.COMPLETE);
		return resource;
	}

	@Override
	protected void checkUpdates(MeasureReport resource)
	{
		assertEquals(MeasureReportStatus.COMPLETE, resource.getStatus());
	}
}
