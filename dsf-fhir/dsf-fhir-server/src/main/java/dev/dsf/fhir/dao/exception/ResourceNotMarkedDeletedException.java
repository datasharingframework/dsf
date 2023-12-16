package dev.dsf.fhir.dao.exception;

public final class ResourceNotMarkedDeletedException extends Exception
{
	private static final long serialVersionUID = 1L;

	private final String id;

	public ResourceNotMarkedDeletedException(String id)
	{
		this.id = id;
	}

	public String getId()
	{
		return id;
	}
}
