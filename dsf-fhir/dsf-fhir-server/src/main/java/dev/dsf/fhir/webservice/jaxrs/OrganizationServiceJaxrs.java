package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.fhir.webservice.specification.OrganizationService;

@Path(OrganizationServiceJaxrs.PATH)
public class OrganizationServiceJaxrs extends AbstractResourceServiceJaxrs<Organization, OrganizationService>
		implements OrganizationService
{
	public static final String PATH = "Organization";

	public OrganizationServiceJaxrs(OrganizationService delegate)
	{
		super(delegate);
	}
}
