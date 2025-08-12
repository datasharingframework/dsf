package dev.dsf.bpe.v2.service;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;

public class DataLoggerImpl implements DataLogger
{
	private static final Logger logger = LoggerFactory.getLogger("dsf-data-logger");

	private final FhirContext fhirContext;

	public DataLoggerImpl(FhirContext fhirContext)
	{
		this.fhirContext = fhirContext;
	}

	public void log(String message, Resource resource)
	{
		if (message != null)
			logger.debug("{}: {}", message, asString(resource));
	}

	@Override
	public void log(String message, Object object)
	{
		if (message != null)
			logger.debug("{}: {}", message, String.valueOf(object));
	}

	private String asString(Resource resource)
	{
		return resource == null ? "null" : fhirContext.newJsonParser().encodeResourceToString(resource);
	}

	@Override
	public boolean isEnabled()
	{
		return logger.isDebugEnabled();
	}
}
