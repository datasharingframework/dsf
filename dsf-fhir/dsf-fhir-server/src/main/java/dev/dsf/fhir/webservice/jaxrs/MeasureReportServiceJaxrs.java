package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.MeasureReport;

import dev.dsf.fhir.webservice.specification.MeasureReportService;

@Path(MeasureReportServiceJaxrs.PATH)
public class MeasureReportServiceJaxrs extends AbstractResourceServiceJaxrs<MeasureReport, MeasureReportService>
		implements MeasureReportService
{
	public static final String PATH = "MeasureReport";

	public MeasureReportServiceJaxrs(MeasureReportService delegate)
	{
		super(delegate);
	}
}
