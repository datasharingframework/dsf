package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.Measure;

import dev.dsf.fhir.webservice.specification.MeasureService;

@Path(MeasureServiceJaxrs.PATH)
public class MeasureServiceJaxrs extends AbstractResourceServiceJaxrs<Measure, MeasureService> implements MeasureService
{
	public static final String PATH = "Measure";

	public MeasureServiceJaxrs(MeasureService delegate)
	{
		super(delegate);
	}
}
