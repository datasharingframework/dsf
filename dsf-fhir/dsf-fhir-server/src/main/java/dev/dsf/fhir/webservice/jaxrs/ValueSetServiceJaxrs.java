package dev.dsf.fhir.webservice.jaxrs;

import javax.ws.rs.Path;

import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.webservice.specification.ValueSetService;

@Path(ValueSetServiceJaxrs.PATH)
public class ValueSetServiceJaxrs extends AbstractResourceServiceJaxrs<ValueSet, ValueSetService>
		implements ValueSetService
{
	public static final String PATH = "ValueSet";

	public ValueSetServiceJaxrs(ValueSetService delegate)
	{
		super(delegate);
	}
}
