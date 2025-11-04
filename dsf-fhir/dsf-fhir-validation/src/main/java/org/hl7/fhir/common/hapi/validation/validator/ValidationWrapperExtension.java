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

import org.hl7.fhir.r5.utils.validation.IValidatorResourceFetcher;
import org.hl7.fhir.r5.utils.validation.constants.BestPracticeWarningLevel;

public class ValidationWrapperExtension extends ValidatorWrapper
{
	public ValidationWrapperExtension()
	{
	}

	public static ValidatorWrapper create(IValidatorResourceFetcher validatorResourceFetcher)
	{
		return new ValidationWrapperExtension().setAnyExtensionsAllowed(true)
				.setBestPracticeWarningLevel(BestPracticeWarningLevel.Ignore).setErrorForUnknownProfiles(true)
				.setExtensionDomains(List.of()).setValidationPolicyAdvisor(new FhirDefaultPolicyAdvisor())
				.setNoTerminologyChecks(false).setNoExtensibleWarnings(false).setNoBindingMsgSuppressed(false)
				.setValidatorResourceFetcher(validatorResourceFetcher).setAssumeValidRestReferences(false);
	}
}
