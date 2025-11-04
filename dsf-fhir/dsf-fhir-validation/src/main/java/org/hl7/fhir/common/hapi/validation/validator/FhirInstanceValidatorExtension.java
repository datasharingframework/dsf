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
package org.hl7.fhir.common.hapi.validation.validator;

import java.util.List;

import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.utils.validation.IValidatorResourceFetcher;
import org.hl7.fhir.utilities.validation.ValidationMessage;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.IValidationContext;

public class FhirInstanceValidatorExtension extends FhirInstanceValidator
{
	private final IValidatorResourceFetcher resourceFetcher;
	private final IWorkerContext workerContext;

	public FhirInstanceValidatorExtension(IValidationSupport validationSupport,
			IValidatorResourceFetcher resourceFetcher, IWorkerContext workerContext)
	{
		super(validationSupport);

		this.resourceFetcher = resourceFetcher;
		this.workerContext = workerContext;
	}

	@Override
	protected List<ValidationMessage> validate(IValidationContext<?> validationContext)
	{
		return ValidationWrapperExtension.create(resourceFetcher).validate(workerContext, validationContext);
	}
}
