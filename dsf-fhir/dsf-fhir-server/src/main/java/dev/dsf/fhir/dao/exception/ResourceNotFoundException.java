package dev.dsf.fhir.dao.exception;

public final class ResourceNotFoundException extends Exception
{
	private static final long serialVersionUID = 1L;

	private final String id;

	public ResourceNotFoundException(String id)
	{
		this.id = id;
	}

	public String getId()
	{
		return id;
	}
}
