package dev.dsf.fhir.validation;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;

public class ResourceInResourceValidator implements ResourceValidator
{
	private static final Pattern BUNDLE_MIN_REQUIRED_FOUND_ZERO_PATTERN = Pattern
			.compile("Bundle.entry: minimum required = (?:[1-9]{1}(?:[0-9]+)?), but only found 0");

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
				Stream.concat(
						bundleResult.getMessages().stream().filter(filterBundleEntryMinimumIfEntriesPresent(entries)),
						entryResults).toList());
	}

	private Predicate<SingleValidationMessage> filterBundleEntryMinimumIfEntriesPresent(
			List<BundleEntryComponent> entries)
	{
		return message -> !(!entries.isEmpty() && ResultSeverityEnum.ERROR.equals(message.getSeverity())
				&& "Bundle".equals(message.getLocationString())
				&& "Validation_VAL_Profile_Minimum".equals(message.getMessageId()) && message.getMessage() != null
				&& BUNDLE_MIN_REQUIRED_FOUND_ZERO_PATTERN.matcher(message.getMessage()).find());
	}
}
