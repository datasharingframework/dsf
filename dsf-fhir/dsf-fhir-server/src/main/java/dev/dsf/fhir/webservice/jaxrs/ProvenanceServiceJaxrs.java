package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.Provenance;

import dev.dsf.fhir.webservice.specification.ProvenanceService;
import jakarta.ws.rs.Path;

@Path(ProvenanceServiceJaxrs.PATH)
public class ProvenanceServiceJaxrs extends AbstractResourceServiceJaxrs<Provenance, ProvenanceService>
		implements ProvenanceService
{
	public static final String PATH = "Provenance";

	public ProvenanceServiceJaxrs(ProvenanceService delegate)
	{
		super(delegate);
	}
}
