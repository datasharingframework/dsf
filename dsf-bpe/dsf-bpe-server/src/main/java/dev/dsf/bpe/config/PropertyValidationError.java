package dev.dsf.bpe.config;

record PropertyValidationError(String property, String message) implements ValidationError
{
	@Override
	public final String toString()
	{
		return "Property {" + property + "} " + message;
	}
}