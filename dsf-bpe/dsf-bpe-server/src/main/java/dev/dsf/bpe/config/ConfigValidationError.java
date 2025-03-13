package dev.dsf.bpe.config;

record ConfigValidationError(String fhirServerId, String message) implements ValidationError
{
	@Override
	public final String toString()
	{
		return "Config {" + fhirServerId + "} " + message;
	}
}