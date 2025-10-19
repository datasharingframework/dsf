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
	 * Validated all bundle entries with a <code>entry.resource</code>. The validation result will be added as a
	 * {@link OperationOutcome} resource to the corresponding <code>entry.response.outcome</code> property.
	 *
	 * @param bundle
	 *            not <code>null</code>
	 * @return given bundle with added <code>entry.response.outcome</code> properties
	 */
	Bundle validate(Bundle bundle);
}
