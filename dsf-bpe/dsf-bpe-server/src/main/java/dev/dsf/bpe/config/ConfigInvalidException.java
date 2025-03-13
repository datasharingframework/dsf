package dev.dsf.bpe.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class ConfigInvalidException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	private final List<ValidationError> validationErrors = new ArrayList<>();

	public ConfigInvalidException(Collection<? extends ValidationError> validationErrors)
	{
		if (validationErrors != null)
			this.validationErrors.addAll(validationErrors);
	}

	public List<ValidationError> getValidationErrors()
	{
		return Collections.unmodifiableList(validationErrors);
	}

	@Override
	public String getMessage()
	{
		return "validation errors: " + validationErrors;
	}
}