package dev.dsf.bpe.config;

import java.util.stream.Stream;

public interface WithValidation
{
	public Stream<ValidationError> validate(String propertyPrefix);
}
