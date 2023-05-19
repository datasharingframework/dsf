package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.HealthcareService;

import dev.dsf.fhir.webservice.specification.HealthcareServiceService;
import jakarta.ws.rs.Path;

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
