package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.CodeSystem;

import dev.dsf.fhir.webservice.specification.CodeSystemService;
import jakarta.ws.rs.Path;

@Path(CodeSystemServiceJaxrs.PATH)
public class CodeSystemServiceJaxrs extends AbstractResourceServiceJaxrs<CodeSystem, CodeSystemService>
		implements CodeSystemService
{
	public static final String PATH = "CodeSystem";

	public CodeSystemServiceJaxrs(CodeSystemService delegate)
	{
		super(delegate);
	}
}
