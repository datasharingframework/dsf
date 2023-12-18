package dev.dsf.fhir.prefer;

public enum PreferReturnType
{
	MINIMAL("return=minimal"), REPRESENTATION("return=representation"), OPERATION_OUTCOME("return=OperationOutcome");

	private final String headerValue;

	PreferReturnType(String headerValue)
	{
		this.headerValue = headerValue;
	}

	public static PreferReturnType fromString(String prefer)
	{
		if (prefer == null)
			return REPRESENTATION;

		return switch (prefer)
		{
			case "return=minimal" -> MINIMAL;
			case "return=OperationOutcome" -> OPERATION_OUTCOME;
			case "return=representation" -> REPRESENTATION;
			default -> REPRESENTATION;
		};
	}

	public String getHeaderValue()
	{
		return headerValue;
	}
}
