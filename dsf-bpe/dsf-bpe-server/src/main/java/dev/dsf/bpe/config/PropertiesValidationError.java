package dev.dsf.bpe.config;

import java.util.List;
import java.util.stream.Collectors;

record PropertiesValidationError(List<String> properties, String message) implements ValidationError
{
	@Override
	public final String toString()
	{
		return "Properties " + properties.stream().collect(Collectors.joining(", ", "{", "} ")) + message;
	}
}