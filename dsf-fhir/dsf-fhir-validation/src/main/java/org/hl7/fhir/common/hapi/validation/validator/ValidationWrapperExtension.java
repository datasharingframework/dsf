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
