/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

	@Override
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
