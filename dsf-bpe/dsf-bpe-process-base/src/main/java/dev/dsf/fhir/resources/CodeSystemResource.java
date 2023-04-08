package dev.dsf.fhir.resources;

import java.util.Objects;

import org.hl7.fhir.r4.model.CodeSystem;

public class CodeSystemResource extends AbstractResource
{
	private CodeSystemResource(String codeSystemFileName)
	{
		super(CodeSystem.class, codeSystemFileName);
	}

	public static CodeSystemResource file(String codeSystemFileName)
	{
		return new CodeSystemResource(Objects.requireNonNull(codeSystemFileName, "codeSystemFileName"));
	}
}
