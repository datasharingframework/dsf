package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.NamingSystem;

import dev.dsf.fhir.webservice.specification.NamingSystemService;

@Path(NamingSystemServiceJaxrs.PATH)
public class NamingSystemServiceJaxrs extends AbstractResourceServiceJaxrs<NamingSystem, NamingSystemService>
		implements NamingSystemService
{
	public static final String PATH = "NamingSystem";

	public NamingSystemServiceJaxrs(NamingSystemService delegate)
	{
		super(delegate);
	}
}
