package dev.dsf.bpe.v2.error;

public class ErrorBoundaryEvent extends RuntimeException
{
	private static final long serialVersionUID = 3161271266680097207L;

	private final String errorCode;
	private final String errorMessage;

	public ErrorBoundaryEvent(String errorCode, String errorMessage)
	{
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	public String getErrorCode()
	{
		return errorCode;
	}

	public String getErrorMessage()
	{
		return errorMessage;
	}

	@Override
	public String getMessage()
	{
		return getErrorCode() + " - " + getErrorMessage();
	}
}
