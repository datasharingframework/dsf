package dev.dsf.fhir.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.ConceptValidationOptions;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.context.support.LookupCodeRequest;
import ca.uhn.fhir.context.support.TranslateConceptResults;
import ca.uhn.fhir.context.support.ValidationSupportContext;
import ca.uhn.fhir.context.support.ValueSetExpansionOptions;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

public class SimpleValidationSupportChain implements IValidationSupport
{
	private final FhirContext fhirContext;
	private final List<IValidationSupport> chain = new ArrayList<>();

	public SimpleValidationSupportChain(FhirContext fhirContext, IValidationSupport... chain)
	{
		this.fhirContext = fhirContext;

		if (chain != null)
			this.chain.addAll(List.of(chain));
	}

	@Override
	public FhirContext getFhirContext()
	{
		return fhirContext;
	}

	private <R> R firstNonNull(Function<IValidationSupport, R> function)
	{
		for (IValidationSupport s : chain)
		{
			R r = function.apply(s);
			if (r != null)
				return r;
		}

		return null;
	}

	private <R> List<R> all(Function<IValidationSupport, List<R>> function)
	{
		List<R> list = new ArrayList<>();
		for (IValidationSupport s : chain)
		{
			List<R> r = function.apply(s);
			if (r != null)
				list.addAll(r);
		}
		return list;
	}

	private boolean checkAll(Predicate<IValidationSupport> predicate)
	{
		for (IValidationSupport s : chain)
		{
			boolean r = predicate.test(s);
			if (r)
				return true;
		}
		return false;
	}

	public ValueSetExpansionOutcome expandValueSet(ValidationSupportContext validationSupportContext,
			ValueSetExpansionOptions expansionOptions, IBaseResource valueSetToExpand)
	{
		return firstNonNull(c -> c.expandValueSet(validationSupportContext, expansionOptions, valueSetToExpand));
	}

	public ValueSetExpansionOutcome expandValueSet(ValidationSupportContext validationSupportContext,
			ValueSetExpansionOptions expansionOptions, String valueSetUrlToExpand) throws ResourceNotFoundException
	{
		return firstNonNull(c -> c.expandValueSet(validationSupportContext, expansionOptions, valueSetUrlToExpand));
	}

	public List<IBaseResource> fetchAllConformanceResources()
	{
		return all(IValidationSupport::fetchAllConformanceResources);
	}

	public <T extends IBaseResource> List<T> fetchAllSearchParameters()
	{
		return all(IValidationSupport::fetchAllSearchParameters);
	}

	public <T extends IBaseResource> List<T> fetchAllStructureDefinitions()
	{
		return all(IValidationSupport::fetchAllStructureDefinitions);
	}

	public <T extends IBaseResource> List<T> fetchAllNonBaseStructureDefinitions()
	{
		return all(IValidationSupport::fetchAllNonBaseStructureDefinitions);
	}

	public IBaseResource fetchCodeSystem(String system)
	{
		return firstNonNull(c -> c.fetchCodeSystem(system));
	}

	public <T extends IBaseResource> T fetchResource(Class<T> clazz, String uri)
	{
		return firstNonNull(c -> c.fetchResource(clazz, uri));
	}

	public IBaseResource fetchStructureDefinition(String url)
	{
		return firstNonNull(c -> c.fetchStructureDefinition(url));
	}

	public boolean isCodeSystemSupported(ValidationSupportContext validationSupportContext, String system)
	{
		return checkAll(c -> c.isCodeSystemSupported(validationSupportContext, system));
	}

	public boolean isRemoteTerminologyServiceConfigured()
	{
		return checkAll(IValidationSupport::isRemoteTerminologyServiceConfigured);
	}

	public IBaseResource fetchValueSet(String url)
	{
		return firstNonNull(c -> c.fetchValueSet(url));
	}

	public byte[] fetchBinary(String binaryKey)
	{
		return firstNonNull(c -> c.fetchBinary(binaryKey));
	}

	public CodeValidationResult validateCode(ValidationSupportContext validationSupportContext,
			ConceptValidationOptions options, String codeSystem, String code, String display, String valueSetUrl)
	{
		return firstNonNull(
				c -> c.validateCode(validationSupportContext, options, codeSystem, code, display, valueSetUrl));
	}

	public CodeValidationResult validateCodeInValueSet(ValidationSupportContext validationSupportContext,
			ConceptValidationOptions options, String codeSystem, String code, String display, IBaseResource valueSet)
	{
		return firstNonNull(
				c -> c.isValueSetSupported(validationSupportContext,
						CommonCodeSystemsTerminologyService.getValueSetUrl(getFhirContext(), valueSet))
								? c.validateCodeInValueSet(validationSupportContext, options, codeSystem, code, display,
										valueSet)
								: null);
	}

	@Deprecated
	public LookupCodeResult lookupCode(ValidationSupportContext validationSupportContext, String system, String code,
			String displayLanguage)
	{
		return firstNonNull(c -> c.lookupCode(validationSupportContext, system, code, displayLanguage));
	}

	@Deprecated
	public LookupCodeResult lookupCode(ValidationSupportContext validationSupportContext, String system, String code)
	{
		return firstNonNull(c -> c.lookupCode(validationSupportContext, system, code));
	}

	public LookupCodeResult lookupCode(ValidationSupportContext validationSupportContext,
			LookupCodeRequest lookupCodeRequest)
	{
		return firstNonNull(c -> c.isCodeSystemSupported(validationSupportContext, lookupCodeRequest.getSystem())
				? c.lookupCode(validationSupportContext, lookupCodeRequest)
				: null);
	}

	public boolean isValueSetSupported(ValidationSupportContext validationSupportContext, String valueSetUrl)
	{
		return firstNonNull(c -> c.isValueSetSupported(validationSupportContext, valueSetUrl));
	}

	public IBaseResource generateSnapshot(ValidationSupportContext validationSupportContext, IBaseResource input,
			String url, String webUrl, String profileName)
	{
		return firstNonNull(c -> c.generateSnapshot(validationSupportContext, input, url, webUrl, profileName));
	}

	public void invalidateCaches()
	{
		chain.forEach(IValidationSupport::invalidateCaches);
	}

	public TranslateConceptResults translateConcept(TranslateCodeRequest request)
	{
		return firstNonNull(c -> c.translateConcept(request));
	}

	public String getName()
	{
		return SimpleValidationSupportChain.class.getSimpleName();
	}

	public boolean isCodeableConceptValidationSuccessfulIfNotAllCodingsAreValid()
	{
		return false;
	}
}
