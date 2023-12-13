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
		switch (value.toLowerCase())
		{
			case "application/fhir+xml":
			case "xml":
				return XML;
			case "application/fhir+json":
			case "json":
				return JSON;
			case "ping":
				return PING;
			default:
				throw new IllegalArgumentException("EvenType for " + value + " not implemented");
		}
	}

	public String toString()
	{
		return value;
	}
}
