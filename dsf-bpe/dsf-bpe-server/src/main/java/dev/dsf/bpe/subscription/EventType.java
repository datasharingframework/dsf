package dev.dsf.bpe.subscription;

public enum EventType
{
	XML("application/fhir+xml"), JSON("application/fhir+json"), PING("ping");

	private final String value;

	EventType(String value)
	{
		this.value = value;
	}

	public static EventType fromString(String value)
	{
		return switch (value.toLowerCase())
		{
			case "application/fhir+xml", "xml" -> XML;
			case "application/fhir+json", "json" -> JSON;
			case "ping" -> PING;

			default -> throw new IllegalArgumentException("EvenType for " + value + " not implemented");
		};
	}

	@Override
	public String toString()
	{
		return value;
	}
}
