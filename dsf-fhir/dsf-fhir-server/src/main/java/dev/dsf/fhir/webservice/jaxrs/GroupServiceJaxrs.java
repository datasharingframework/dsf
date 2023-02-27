package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.Group;

import dev.dsf.fhir.webservice.specification.GroupService;

@Path(GroupServiceJaxrs.PATH)
public class GroupServiceJaxrs extends AbstractResourceServiceJaxrs<Group, GroupService> implements GroupService
{
	public static final String PATH = "Group";

	public GroupServiceJaxrs(GroupService delegate)
	{
		super(delegate);
	}
}
