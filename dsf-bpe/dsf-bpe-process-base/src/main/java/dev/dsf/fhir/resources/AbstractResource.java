package dev.dsf.fhir.resources;

import org.hl7.fhir.r4.model.MetadataResource;

public abstract class AbstractResource
{
	private final Class<? extends MetadataResource> type;
	private final String fileName;

	AbstractResource(Class<? extends MetadataResource> type, String fileName)
	{
		this.type = type;
		this.fileName = fileName;
	}

	public Class<? extends MetadataResource> getType()
	{
		return type;
	}

	public String getFileName()
	{
		return fileName;
	}
}
