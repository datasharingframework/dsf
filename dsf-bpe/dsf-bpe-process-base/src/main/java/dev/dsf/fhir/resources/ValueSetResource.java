package dev.dsf.fhir.resources;

import java.util.Objects;

import org.hl7.fhir.r4.model.ValueSet;

public class ValueSetResource extends AbstractResource
{
	private ValueSetResource(String valueSetFileName)
	{
		super(ValueSet.class, valueSetFileName);
	}

	public static ValueSetResource file(String valueSetFileName)
	{
		return new ValueSetResource(Objects.requireNonNull(valueSetFileName, "valueSetFileName"));
	}
}
