package dev.dsf.fhir.search;

public class SearchQueryParameterError
{
	public static enum SearchQueryParameterErrorType
	{
		UNSUPPORTED_PARAMETER, UNSUPPORTED_NUMBER_OF_VALUES, UNPARSABLE_VALUE
	}

	private final SearchQueryParameterErrorType type;
	private final String parameterName;
	private final String parameterValue;
	private final Exception exception;
	private final String message;

	public SearchQueryParameterError(SearchQueryParameterErrorType type, String parameterName, String parameterValue)
	{
		this(type, parameterName, parameterValue, null, null);
	}

	public SearchQueryParameterError(SearchQueryParameterErrorType type, String parameterName, String parameterValue,
			String message)
	{
		this(type, parameterName, parameterValue, null, message);
	}

	public SearchQueryParameterError(SearchQueryParameterErrorType type, String parameterName, String parameterValue,
			Exception exception)
	{
		this(type, parameterName, parameterValue, exception, null);
	}

	public SearchQueryParameterError(SearchQueryParameterErrorType type, String parameterName, String parameterValue,
			Exception exception, String message)
	{
		this.type = type;
		this.parameterName = parameterName;
		this.parameterValue = parameterValue;
		this.exception = exception;
		this.message = message;
	}

	public SearchQueryParameterErrorType getType()
	{
		return type;
	}

	public String getParameterName()
	{
		return parameterName;
	}

	public String getParameterValue()
	{
		return parameterValue;
	}

	public Exception getException()
	{
		return exception;
	}

	public String getMessage()
	{
		return message;
	}

	@Override
	public String toString()
	{
		StringBuilder b = new StringBuilder("parameter: ");
		b.append(parameterName);
		b.append(", error: ");
		b.append(type);

		if (exception != null || message != null)
		{
			b.append(", message: '");
			if (exception != null)
			{
				b.append(exception.getClass().getSimpleName());
				b.append(" - ");
				b.append(exception.getMessage());
				b.append("'");
			}
			else if (message != null)
			{
				b.append(message);
				b.append("'");
			}
		}
		if (parameterValue != null)
		{
			b.append(", value: ");
			b.append(parameterValue);
		}

		return b.toString();
	}
}
