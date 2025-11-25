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
package dev.dsf.bpe.v2.service.validation;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.validation.ValidationResult;

public interface ValidationService
{
	/**
	 * Validates against profiles self reported by the given <b>resource</b>.
	 *
	 * @param resource
	 *            not <code>null</code>
	 * @return validation results
	 */
	ValidationResult validate(Resource resource);

	/**
	 * Validates the given <b>resource</b> against the given <b>profileUrl</b>.
	 *
	 * @param resource
	 *            not <code>null</code>
	 * @param profileUrl
	 *            not <code>null</code>, not blank
	 * @return validation results
	 */
	ValidationResult validate(Resource resource, String profileUrl);

	/**
	 * Validates all bundle entries with a <code>entry.resource</code> against self reported profiles. Validation result
	 * is added as a {@link OperationOutcome} resource to the corresponding <code>entry.response.outcome</code>
	 * property.
	 *
	 * @param bundle
	 *            not <code>null</code>
	 * @return given bundle with added <code>entry.response.outcome</code> properties
	 */
	Bundle validateEntries(Bundle bundle);
}
