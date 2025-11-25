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

import java.util.Optional;
import java.util.function.Predicate;

import dev.dsf.bpe.v2.service.validation.FhirPackageIdentifier;
import dev.dsf.bpe.v2.service.validation.ValidationService;

public interface ValidationServiceProvider
{
	/**
	 * Returns a {@link ValidationService} configured for the given FHIR package <b>identifiers</b> and their
	 * dependencies.
	 *
	 * @param identifiers
	 *            not <code>null</code>, not empty
	 * @return {@link Optional#empty()} if resource validation is disabled for the DSF instance
	 */
	default Optional<ValidationService> getValidationService(FhirPackageIdentifier... identifiers)
	{
		return getValidationService(_ -> true, identifiers);
	}

	/**
	 * Returns a {@link ValidationService} configured for the given FHIR package <b>identifiers</b> and their
	 * dependencies. Excludes packages when the given <b>filter</b> returns <code>false</code>.
	 *
	 * @param filter
	 *            not <code>null</code>, packages are only included if allowed by this filter
	 * @param identifiers
	 *            not <code>null</code>, not empty
	 * @return {@link Optional#empty()} if resource validation is disabled for the DSF instance
	 */
	Optional<ValidationService> getValidationService(Predicate<FhirPackageIdentifier> filter,
			FhirPackageIdentifier... identifiers);
}
