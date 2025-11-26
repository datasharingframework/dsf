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

	@Override
	public ValueSetExpansionOutcome expandValueSet(ValidationSupportContext validationSupportContext,
			ValueSetExpansionOptions expansionOptions, IBaseResource valueSetToExpand)
	{
		return firstNonNull(c -> c.expandValueSet(validationSupportContext, expansionOptions, valueSetToExpand));
	}

	@Override
	public ValueSetExpansionOutcome expandValueSet(ValidationSupportContext validationSupportContext,
			ValueSetExpansionOptions expansionOptions, String valueSetUrlToExpand) throws ResourceNotFoundException
	{
		return firstNonNull(c -> c.expandValueSet(validationSupportContext, expansionOptions, valueSetUrlToExpand));
	}

	@Override
	public List<IBaseResource> fetchAllConformanceResources()
	{
		return all(IValidationSupport::fetchAllConformanceResources);
	}

	@Override
	public <T extends IBaseResource> List<T> fetchAllSearchParameters()
	{
		return all(IValidationSupport::fetchAllSearchParameters);
	}

	@Override
	public <T extends IBaseResource> List<T> fetchAllStructureDefinitions()
	{
		return all(IValidationSupport::fetchAllStructureDefinitions);
	}

	@Override
	public <T extends IBaseResource> List<T> fetchAllNonBaseStructureDefinitions()
	{
		return all(IValidationSupport::fetchAllNonBaseStructureDefinitions);
	}

	@Override
	public IBaseResource fetchCodeSystem(String system)
	{
		return firstNonNull(c -> c.fetchCodeSystem(system));
	}

	@Override
	public <T extends IBaseResource> T fetchResource(Class<T> clazz, String uri)
	{
		return firstNonNull(c -> c.fetchResource(clazz, uri));
	}

	@Override
	public IBaseResource fetchStructureDefinition(String url)
	{
		return firstNonNull(c -> c.fetchStructureDefinition(url));
	}

	@Override
	public boolean isCodeSystemSupported(ValidationSupportContext validationSupportContext, String system)
	{
		return checkAll(c -> c.isCodeSystemSupported(validationSupportContext, system));
	}

	@Override
	public boolean isRemoteTerminologyServiceConfigured()
	{
		return checkAll(IValidationSupport::isRemoteTerminologyServiceConfigured);
	}

	@Override
	public IBaseResource fetchValueSet(String url)
	{
		return firstNonNull(c -> c.fetchValueSet(url));
	}

	@Override
	public byte[] fetchBinary(String binaryKey)
	{
		return firstNonNull(c -> c.fetchBinary(binaryKey));
	}

	@Override
	public CodeValidationResult validateCode(ValidationSupportContext validationSupportContext,
			ConceptValidationOptions options, String codeSystem, String code, String display, String valueSetUrl)
	{
		return firstNonNull(
				c -> c.validateCode(validationSupportContext, options, codeSystem, code, display, valueSetUrl));
	}

	@Override
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
	@Override
	public LookupCodeResult lookupCode(ValidationSupportContext validationSupportContext, String system, String code,
			String displayLanguage)
	{
		return firstNonNull(c -> c.lookupCode(validationSupportContext, system, code, displayLanguage));
	}

	@Deprecated
	@Override
	public LookupCodeResult lookupCode(ValidationSupportContext validationSupportContext, String system, String code)
	{
		return firstNonNull(c -> c.lookupCode(validationSupportContext, system, code));
	}

	@Override
	public LookupCodeResult lookupCode(ValidationSupportContext validationSupportContext,
			LookupCodeRequest lookupCodeRequest)
	{
		return firstNonNull(c -> c.isCodeSystemSupported(validationSupportContext, lookupCodeRequest.getSystem())
				? c.lookupCode(validationSupportContext, lookupCodeRequest)
				: null);
	}

	@Override
	public boolean isValueSetSupported(ValidationSupportContext validationSupportContext, String valueSetUrl)
	{
		return firstNonNull(c -> c.isValueSetSupported(validationSupportContext, valueSetUrl));
	}

	@Override
	public IBaseResource generateSnapshot(ValidationSupportContext validationSupportContext, IBaseResource input,
			String url, String webUrl, String profileName)
	{
		return firstNonNull(c -> c.generateSnapshot(validationSupportContext, input, url, webUrl, profileName));
	}

	@Override
	public void invalidateCaches()
	{
		chain.forEach(IValidationSupport::invalidateCaches);
	}

	@Override
	public TranslateConceptResults translateConcept(TranslateCodeRequest request)
	{
		return firstNonNull(c -> c.translateConcept(request));
	}

	@Override
	public String getName()
	{
		return SimpleValidationSupportChain.class.getSimpleName();
	}

	@Override
	public boolean isCodeableConceptValidationSuccessfulIfNotAllCodingsAreValid()
	{
		return false;
	}
}
