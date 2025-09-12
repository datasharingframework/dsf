package dev.dsf.common.oidc;

public class JwksException extends OidcClientException
{
	private static final long serialVersionUID = 1L;

	public JwksException(String message)
	{
		super(message);
	}

	public JwksException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
