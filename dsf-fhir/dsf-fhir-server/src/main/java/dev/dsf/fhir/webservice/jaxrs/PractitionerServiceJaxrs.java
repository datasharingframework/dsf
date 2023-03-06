package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.fhir.webservice.specification.PractitionerService;
import jakarta.ws.rs.Path;

@Path(PractitionerServiceJaxrs.PATH)
public class PractitionerServiceJaxrs extends AbstractResourceServiceJaxrs<Practitioner, PractitionerService>
		implements PractitionerService
{
	public static final String PATH = "Practitioner";

	public PractitionerServiceJaxrs(PractitionerService delegate)
	{
		super(delegate);
	}
}
