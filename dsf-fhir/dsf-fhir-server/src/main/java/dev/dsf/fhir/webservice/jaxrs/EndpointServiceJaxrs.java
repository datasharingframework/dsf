package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.Endpoint;

import dev.dsf.fhir.webservice.specification.EndpointService;

@Path(EndpointServiceJaxrs.PATH)
public class EndpointServiceJaxrs extends AbstractResourceServiceJaxrs<Endpoint, EndpointService>
		implements EndpointService
{
	public static final String PATH = "Endpoint";

	public EndpointServiceJaxrs(EndpointService delegate)
	{
		super(delegate);
	}
}
