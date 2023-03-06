package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Measure;

import dev.dsf.fhir.webservice.specification.MeasureService;
import jakarta.ws.rs.Path;

@Path(MeasureServiceJaxrs.PATH)
public class MeasureServiceJaxrs extends AbstractResourceServiceJaxrs<Measure, MeasureService> implements MeasureService
{
	public static final String PATH = "Measure";

	public MeasureServiceJaxrs(MeasureService delegate)
	{
		super(delegate);
	}
}
