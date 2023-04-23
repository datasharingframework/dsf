package dev.dsf.fhir.help;

public enum SummaryMode
{
	TRUE, TEXT, DATA, COUNT, FALSE;

	public static SummaryMode fromString(String mode)
	{
		if (mode == null)
			return null;

		return switch (mode.toLowerCase())
		{
			case "true" -> SummaryMode.TRUE;
			case "text" -> SummaryMode.TEXT;
			case "data" -> SummaryMode.DATA;
			case "count" -> SummaryMode.COUNT;
			case "false" -> SummaryMode.FALSE;
			default -> null;
		};
	}

	public static boolean isValid(String mode)
	{
		return fromString(mode) != null;
	}

	@Override
	public String toString()
	{
		return name().toLowerCase();
	}
}