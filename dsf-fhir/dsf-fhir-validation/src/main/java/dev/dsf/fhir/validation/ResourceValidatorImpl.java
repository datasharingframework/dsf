package dev.dsf.fhir.validation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidatorExtension;
import org.hl7.fhir.common.hapi.validation.validator.FixedVersionSpecificWorkerContextWrapper;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.context.IWorkerContext;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.utils.validation.IResourceValidator;
import org.hl7.fhir.r5.utils.validation.IValidatorResourceFetcher;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;

public class ResourceValidatorImpl implements ResourceValidator
{
	private static final Pattern AT_DEFAULT_SLICE_PATTERN = Pattern
			.compile(".*(Questionnaire|QuestionnaireResponse).item:@default.*");

	private static final String MISSING_NARRATIVE_MESSAGE_START = "Constraint failed: dom-6: 'A resource should have narrative for robust management'";

	private static final class ValidatorResourceFetcher implements IValidatorResourceFetcher
	{
		@Override
		public IValidatorResourceFetcher setLocale(Locale locale)
		{
			return this;
		}

		@Override
		public boolean resolveURL(IResourceValidator validator, Object appContext, String path, String url, String type,
				boolean canonical) throws IOException, FHIRException
		{
			if (("urn:ietf:bcp:13".equals(url) || "urn:ietf:bcp:13|4.0.1".equals(url)
					|| "urn:ietf:rfc:3986".equals(url)) && "uri".equals(type) && !canonical)
				return true;
			else if (url != null && url.startsWith("urn:uuid:") && url.length() == 45
					&& ("uri".equals(type) || "url".equals(type)) && !canonical)
				return true;
			else if (url != null && (url.startsWith("http://") || url.startsWith("https://"))
					&& ("uri".equals(type) || "canonical".equals(type)))
				return true;
			else if (path != null && (path.startsWith("ActivityDefinition") || path.startsWith("Binary")
					|| path.startsWith("Bundle") || path.startsWith("CodeSystem")
					|| path.startsWith("DocumentReference") || path.startsWith("Endpoint") || path.startsWith("Library")
					|| path.startsWith("Organization") || path.startsWith("QuestionnaireResponse")
					|| path.startsWith("ResearchStudy") || path.startsWith("StructureDefinition")
					|| path.startsWith("Task")))
				return true;

			System.err.println("!!!!!!! " + path + ", " + url + ", " + type + ", " + canonical);
			return false;
		}

		@Override
		public boolean fetchesCanonicalResource(IResourceValidator validator, String url)
		{
			return false;
		}

		@Override
		public byte[] fetchRaw(IResourceValidator validator, String url) throws IOException
		{
			return null;
		}

		@Override
		public Set<String> fetchCanonicalResourceVersions(IResourceValidator validator, Object appContext, String url)
		{
			return Set.of();
		}

		@Override
		public CanonicalResource fetchCanonicalResource(IResourceValidator validator, Object appContext, String url)
				throws URISyntaxException
		{
			return null;
		}

		@Override
		public org.hl7.fhir.r5.elementmodel.Element fetch(IResourceValidator validator, Object appContext, String url)
				throws FHIRException, IOException
		{
			return null;
		}
	}

	private final FhirContext context;
	private final FhirValidator validator;

	public ResourceValidatorImpl(FhirContext context, IValidationSupport validationSupport)
	{
		this.context = context;
		this.validator = configureValidator(context, validationSupport);
	}

	protected FhirValidator configureValidator(FhirContext fhirContext, IValidationSupport validationSupport)
	{
		FhirValidator validator = fhirContext.newValidator();

		IWorkerContext workerContext = FixedVersionSpecificWorkerContextWrapper
				.newVersionSpecificWorkerContextWrapper(validationSupport);

		FhirInstanceValidator instanceValidator = new FhirInstanceValidatorExtension(validationSupport,
				new ValidatorResourceFetcher(), workerContext);

		validator.registerValidatorModule(instanceValidator);
		return validator;
	}

	@Override
	public ValidationResult validate(Resource resource)
	{
		ValidationResult result = validator.validateWithResult(resource);

		// TODO: remove after HAPI validator is fixed: https://github.com/hapifhir/org.hl7.fhir.core/issues/193
		adaptDefaultSliceValidationErrorToWarning(result);

		return new ValidationResult(context,
				result.getMessages().stream().filter(m -> !(ResultSeverityEnum.WARNING.equals(m.getSeverity())
						&& m.getMessage().startsWith(MISSING_NARRATIVE_MESSAGE_START))).toList());
	}

	private void adaptDefaultSliceValidationErrorToWarning(ValidationResult result)
	{
		result.getMessages().stream()
				.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
						&& AT_DEFAULT_SLICE_PATTERN.matcher(m.getMessage()).matches())
				.forEach(m -> m.setSeverity(ResultSeverityEnum.WARNING));
	}
}
