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
package dev.dsf.fhir.validation;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidatorExtension;
import org.hl7.fhir.common.hapi.validation.validator.WorkerContextValidationSupportAdapter;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.model.CanonicalResource;
import org.hl7.fhir.r5.model.CanonicalType;
import org.hl7.fhir.r5.model.CodeSystem;
import org.hl7.fhir.r5.model.Coding;
import org.hl7.fhir.r5.model.ValueSet;
import org.hl7.fhir.r5.utils.validation.IResourceValidator;
import org.hl7.fhir.r5.utils.validation.IValidatorResourceFetcher;
import org.hl7.fhir.utilities.validation.ValidationOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.ValidationResult;
import ca.uhn.hapi.converters.canonical.VersionCanonicalizer;

public class ResourceValidatorImpl implements ResourceValidator
{
	private static final Logger logger = LoggerFactory.getLogger(ResourceValidatorImpl.class);

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
				boolean canonical, List<CanonicalType> targets) throws IOException, FHIRException
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
					|| path.startsWith("DocumentReference") || path.startsWith("ElementDefinition")
					|| path.startsWith("Endpoint") || path.startsWith("Extension") || path.startsWith("Group")
					|| path.startsWith("HealthcareService") || path.startsWith("Library") || path.startsWith("Location")
					|| path.startsWith("Measure") || path.startsWith("MeasureReport") || path.startsWith("NamingSystem")
					|| path.startsWith("Organization") || path.startsWith("OrganizationAffiliation")
					|| path.startsWith("Patient") || path.startsWith("Practitioner")
					|| path.startsWith("PractitionerRole") || path.startsWith("Provenance")
					|| path.startsWith("Questionnaire") || path.startsWith("QuestionnaireResponse")
					|| path.startsWith("Reference") || path.startsWith("ResearchStudy")
					|| path.startsWith("StructureDefinition") || path.startsWith("Subscription")
					|| path.startsWith("Task") || path.startsWith("ValueSet")))
				return true;

			logger.debug("Not resolving [path: {}, url: {}, type: {}, canonical: {}]", path, url, type, canonical);
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

		VersionCanonicalizer versionCanonicalizer = new VersionCanonicalizer(validationSupport.getFhirContext());
		ValidationSupportContext validationSupportContext = new ValidationSupportContext(validationSupport);
		WorkerContextValidationSupportAdapter workerContext = new WorkerContextValidationSupportAdapter(
				validationSupport)
		{
			@Override
			public CodeSystem fetchCodeSystem(String system)
			{
				// workaround to disable validation against non version specific code systems
				return null;
			}

			@Override
			public CodeSystem fetchCodeSystem(String system, String version)
			{
				IBaseResource fetched = validationSupportContext.getRootValidationSupport()
						.fetchCodeSystem(version != null && !version.isBlank() ? (system + "|" + version) : system);

				try
				{
					return fetched == null ? null : versionCanonicalizer.codeSystemToValidatorCanonical(fetched);
				}
				catch (FHIRException e)
				{
					throw new InternalErrorException(Msg.code(1992) + e);
				}
			}

			@Override
			public org.hl7.fhir.r5.terminologies.utilities.ValidationResult validateCode(ValidationOptions theOptions,
					Coding theCoding, ValueSet theValueSet)
			{
				String system = theCoding.getSystem();
				String version = theCoding.getVersion();

				if (version != null && !version.isBlank())
					theCoding.setSystem(theCoding.getSystem() + "|" + version);

				org.hl7.fhir.r5.terminologies.utilities.ValidationResult result = super.validateCode(theOptions,
						theCoding, theValueSet);

				theCoding.setSystem(system);

				return result;
			}
		};

		FhirInstanceValidator instanceValidator = new FhirInstanceValidatorExtension(validationSupport,
				new ValidatorResourceFetcher(), workerContext);

		validator.registerValidatorModule(instanceValidator);
		return validator;
	}

	@Override
	public ValidationResult validate(Resource resource)
	{
		ValidationResult result = validator.validateWithResult(resource);

		// TODO remove after HAPI validator is fixed: https://github.com/hapifhir/org.hl7.fhir.core/issues/193
		adaptDefaultSliceValidationErrorToWarning(result);
		adaptQuestionnaireTextNotSameValidationErrorToWarning(result);

		// TODO remove if MII plugins with error no longer in circulation
		adaptMiiPluginsBundleFullUrlUuidNullErrorToWarning(resource, result);

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

	private void adaptQuestionnaireTextNotSameValidationErrorToWarning(ValidationResult result)
	{
		result.getMessages().stream()
				.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity()) && m.getMessage()
						.startsWith("If text exists, it must match the questionnaire definition for linkId"))
				.forEach(m -> m.setSeverity(ResultSeverityEnum.WARNING));
	}

	private void adaptMiiPluginsBundleFullUrlUuidNullErrorToWarning(Resource resource, ValidationResult result)
	{
		if (resource instanceof Bundle b && b.hasIdentifier()
				&& "http://medizininformatik-initiative.de/fhir/CodeSystem/cryptography"
						.equals(b.getIdentifier().getSystem())
				&& "public-key".equals(b.getIdentifier().getValue()))
		{
			result.getMessages().stream()
					.filter(m -> ResultSeverityEnum.ERROR.equals(m.getSeverity())
							&& "UUIDs must be valid and lowercase (null)".equals(m.getMessage()))
					.forEach(m -> m.setSeverity(ResultSeverityEnum.WARNING));
		}
	}
}
