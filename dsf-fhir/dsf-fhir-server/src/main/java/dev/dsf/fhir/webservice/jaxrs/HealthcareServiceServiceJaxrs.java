package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.HealthcareService;

import dev.dsf.fhir.webservice.specification.HealthcareServiceService;

@Path(HealthcareServiceServiceJaxrs.PATH)
public class HealthcareServiceServiceJaxrs extends
		AbstractResourceServiceJaxrs<HealthcareService, HealthcareServiceService> implements HealthcareServiceService
{
	public static final String PATH = "HealthcareService";

	public HealthcareServiceServiceJaxrs(HealthcareServiceService delegate)
	{
		super(delegate);
	}
}
