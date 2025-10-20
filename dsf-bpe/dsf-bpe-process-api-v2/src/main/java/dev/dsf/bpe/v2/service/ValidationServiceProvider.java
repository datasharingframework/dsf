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
