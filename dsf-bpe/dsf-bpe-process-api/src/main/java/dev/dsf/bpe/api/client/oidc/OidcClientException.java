package dev.dsf.bpe.api.client.oidc;

public class OidcClientException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	public OidcClientException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public OidcClientException(String message)
	{
		super(message);
	}
}
