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
