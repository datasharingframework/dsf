package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.MeasureReport;

import dev.dsf.fhir.webservice.specification.MeasureReportService;
import jakarta.ws.rs.Path;

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
