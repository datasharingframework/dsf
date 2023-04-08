package dev.dsf.fhir.resources;

import java.util.Objects;

import org.hl7.fhir.r4.model.NamingSystem;

public class NamingSystemResource extends AbstractResource
{
	private NamingSystemResource(String namingSystemFileName)
	{
		super(NamingSystem.class, namingSystemFileName);
	}

	public static NamingSystemResource file(String namingSystemFileName)
	{
		return new NamingSystemResource(Objects.requireNonNull(namingSystemFileName, "namingSystemFileName"));
	}
}
