package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.fhir.webservice.specification.OrganizationAffiliationService;

@Path(OrganizationAffiliationServiceJaxrs.PATH)
public class OrganizationAffiliationServiceJaxrs
		extends AbstractResourceServiceJaxrs<OrganizationAffiliation, OrganizationAffiliationService>
		implements OrganizationAffiliationService
{
	public static final String PATH = "OrganizationAffiliation";

	public OrganizationAffiliationServiceJaxrs(OrganizationAffiliationService delegate)
	{
		super(delegate);
	}
}
