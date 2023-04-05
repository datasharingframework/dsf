package dev.dsf.fhir.resources;

import java.util.Objects;

import org.hl7.fhir.r4.model.StructureDefinition;

public class StructureDefinitionResource extends AbstractResource
{
	private StructureDefinitionResource(String structureDefinitionFileName)
	{
		super(StructureDefinition.class, structureDefinitionFileName);
	}

	public static StructureDefinitionResource file(String structureDefinitionFileName)
	{
		return new StructureDefinitionResource(
				Objects.requireNonNull(structureDefinitionFileName, "structureDefinitionFileName"));
	}
}
