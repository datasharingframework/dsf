package dev.dsf.fhir.dao.exception;

public final class BadBundleException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public BadBundleException(String message)
	{
		super(message);
	}
}
