package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.PractitionerRole;

import dev.dsf.fhir.webservice.specification.PractitionerRoleService;

@Path(PractitionerRoleServiceJaxrs.PATH)
public class PractitionerRoleServiceJaxrs extends
		AbstractResourceServiceJaxrs<PractitionerRole, PractitionerRoleService> implements PractitionerRoleService
{
	public static final String PATH = "PractitionerRole";

	public PractitionerRoleServiceJaxrs(PractitionerRoleService delegate)
	{
		super(delegate);
	}
}
