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

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.bpe.v2.service.validation.FhirPackageIdentifier;
import dev.dsf.bpe.v2.service.validation.ValidationService;

public class ValidationServiceProviderImpl implements ValidationServiceProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ValidationServiceProviderImpl.class);

	private final boolean enabled;
	private final FhirContext fhirContext;

	public ValidationServiceProviderImpl(boolean enabled, FhirContext fhirContext)
	{
		this.enabled = enabled;
		this.fhirContext = fhirContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
	}

	@Override
	public Optional<ValidationService> getValidationService(Predicate<FhirPackageIdentifier> filter,
			FhirPackageIdentifier... identifiers)
	{
		if (!enabled)
			return Optional.empty();
		else
		{
			// TODO implement validation, add needed HAPI dependencies

			return Optional.of(new ValidationService()
			{
				@Override
				public Bundle validateEntries(Bundle bundle)
				{
					logger.warn("Bundle validation not implemented, returning bundle as is");

					return bundle;
				}

				@Override
				public ValidationResult validate(Resource resource, String profileUrl)
				{
					return validate(resource);
				}

				@Override
				public ValidationResult validate(Resource resource)
				{
					logger.warn(
							"Resource validation not implemented, returning successful validation result without messages");

					return new ValidationResult(fhirContext, List.of());
				}
			});
		}
	}
}
