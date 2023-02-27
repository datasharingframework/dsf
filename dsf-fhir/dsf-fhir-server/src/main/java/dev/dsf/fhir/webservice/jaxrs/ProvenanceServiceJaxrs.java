package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.Provenance;

import dev.dsf.fhir.webservice.specification.ProvenanceService;

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
