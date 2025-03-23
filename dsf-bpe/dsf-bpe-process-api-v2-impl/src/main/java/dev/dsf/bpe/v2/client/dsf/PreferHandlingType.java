package dev.dsf.bpe.v2.client.dsf;

public enum PreferHandlingType
{
	STRICT("handling=strict"), LENIENT("handling=lenient");

	private final String headerValue;

	PreferHandlingType(String headerValue)
	{
		this.headerValue = headerValue;
	}

	public static PreferHandlingType fromString(String prefer)
	{
		if (prefer == null)
			return LENIENT;

		return switch (prefer)
		{
			case "handling=strict" -> STRICT;
			case "handling=lenient" -> LENIENT;
			default -> LENIENT;
		};
	}

	public String getHeaderValue()
	{
		return headerValue;
	}
}
