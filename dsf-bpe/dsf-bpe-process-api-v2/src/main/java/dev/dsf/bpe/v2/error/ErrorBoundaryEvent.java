package dev.dsf.bpe.v2.error;

import java.util.Objects;

public class ErrorBoundaryEvent extends RuntimeException
{
	private static final long serialVersionUID = 3161271266680097207L;

	private final String errorCode;
	private final String errorMessage;

	/**
	 * @param errorCode
	 *            not <code>null</code>, not empty
	 * @param errorMessage
	 *            not <code>null</code>, not empty
	 */
	public ErrorBoundaryEvent(String errorCode, String errorMessage)
	{
		this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
		this.errorMessage = Objects.requireNonNull(errorMessage, "errorMessage");

		if (errorCode.isEmpty())
			throw new IllegalArgumentException("errorCode empty");
		if (errorMessage.isEmpty())
			throw new IllegalArgumentException("errorMessage empty");
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
