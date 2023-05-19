package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.webservice.specification.ValueSetService;
import jakarta.ws.rs.Path;

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
