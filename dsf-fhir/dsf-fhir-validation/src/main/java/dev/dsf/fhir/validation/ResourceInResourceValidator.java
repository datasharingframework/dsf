package dev.dsf.fhir.validation;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;

public class ResourceInResourceValidator implements ResourceValidator
{
	private final FhirContext fhirContext;
	private final ResourceValidator delegate;

	public ResourceInResourceValidator(FhirContext fhirContext, ResourceValidator delegate)
	{
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public ValidationResult validate(Resource resource)
	{
		if (resource instanceof Bundle b)
			return validateBundle(b);
		else
			return delegate.validate(resource);
	}

	// XXX Workaround for HAPI bug, unable to validating Bundles containing Bundles
	// Bug may be related to https://github.com/hapifhir/org.hl7.fhir.core/issues/1889
	private ValidationResult validateBundle(Bundle bundle)
	{
		List<BundleEntryComponent> entries = bundle.getEntry();
		bundle.setEntry(null);
		ValidationResult bundleResult = delegate.validate(bundle);
		bundle.setEntry(entries);

		Stream<SingleValidationMessage> entryResults = entries.stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).map(this::validate).map(ValidationResult::getMessages)
				.flatMap(List::stream);

		return new ValidationResult(fhirContext,
				Stream.concat(bundleResult.getMessages().stream(), entryResults).toList());
	}
}
