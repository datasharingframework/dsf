package dev.dsf.fhir.resources;

import java.util.Objects;

import org.hl7.fhir.r4.model.ActivityDefinition;

public class ActivityDefinitionResource extends AbstractResource
{
	private ActivityDefinitionResource(String fileName)
	{
		super(ActivityDefinition.class, fileName);
	}

	public static ActivityDefinitionResource file(String fileName)
	{
		return new ActivityDefinitionResource(Objects.requireNonNull(fileName, "fileName"));
	}
}
