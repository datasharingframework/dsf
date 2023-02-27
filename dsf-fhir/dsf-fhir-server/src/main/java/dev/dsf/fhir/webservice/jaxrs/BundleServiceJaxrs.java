package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.Bundle;

import dev.dsf.fhir.webservice.specification.BundleService;

@Path(BundleServiceJaxrs.PATH)
public class BundleServiceJaxrs extends AbstractResourceServiceJaxrs<Bundle, BundleService> implements BundleService
{
	public static final String PATH = "Bundle";

	public BundleServiceJaxrs(BundleService delegate)
	{
		super(delegate);
	}
}
