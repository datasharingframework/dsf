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
