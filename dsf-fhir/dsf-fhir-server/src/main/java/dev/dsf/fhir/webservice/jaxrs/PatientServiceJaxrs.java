package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Patient;

import dev.dsf.fhir.webservice.specification.PatientService;
import jakarta.ws.rs.Path;

@Path(PatientServiceJaxrs.PATH)
public class PatientServiceJaxrs extends AbstractResourceServiceJaxrs<Patient, PatientService> implements PatientService
{
	public static final String PATH = "Patient";

	public PatientServiceJaxrs(PatientService delegate)
	{
		super(delegate);
	}
}
