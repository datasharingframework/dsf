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
package dev.dsf.bpe.v2.client.fhir;

import java.util.Collection;
import java.util.Set;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IPrimitiveType;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.context.IFhirValidatorFactory;
import ca.uhn.fhir.context.ParserOptions;
import ca.uhn.fhir.context.PerformanceOptionsEnum;
import ca.uhn.fhir.context.RuntimeChildUndeclaredExtensionDefinition;
import ca.uhn.fhir.context.RuntimeResourceDefinition;
import ca.uhn.fhir.context.api.AddProfileTagEnum;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.i18n.HapiLocalizer;
import ca.uhn.fhir.model.api.IFhirVersion;
import ca.uhn.fhir.model.view.ViewGenerator;
import ca.uhn.fhir.narrative.INarrativeGenerator;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.IParserErrorHandler;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.IRestfulClient;
import ca.uhn.fhir.rest.client.api.IRestfulClientFactory;
import ca.uhn.fhir.util.FhirTerser;
import ca.uhn.fhir.validation.FhirValidator;

public class FhirContextDelegate extends FhirContext
{
	private final FhirContext delegate;

	private IRestfulClientFactory restfulClientFactory;

	@SuppressWarnings("deprecation")
	public FhirContextDelegate(FhirContext delegate)
	{
		super();

		this.delegate = delegate;
	}

	@Override
	public int hashCode()
	{
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj)
	{
		return delegate.equals(obj);
	}

	@Override
	public AddProfileTagEnum getAddProfileTagWhenEncoding()
	{
		return delegate.getAddProfileTagWhenEncoding();
	}

	@Override
	public void setAddProfileTagWhenEncoding(AddProfileTagEnum theAddProfileTagWhenEncoding)
	{
		delegate.setAddProfileTagWhenEncoding(theAddProfileTagWhenEncoding);
	}

	@Override
	public Class<? extends IBaseResource> getDefaultTypeForProfile(String theProfile)
	{
		return delegate.getDefaultTypeForProfile(theProfile);
	}

	@Override
	public BaseRuntimeElementDefinition<?> getElementDefinition(Class<? extends IBase> theElementType)
	{
		return delegate.getElementDefinition(theElementType);
	}

	@Override
	public BaseRuntimeElementDefinition<?> getElementDefinition(String theElementName)
	{
		return delegate.getElementDefinition(theElementName);
	}

	@Override
	public Collection<BaseRuntimeElementDefinition<?>> getElementDefinitions()
	{
		return delegate.getElementDefinitions();
	}

	@Override
	public HapiLocalizer getLocalizer()
	{
		return delegate.getLocalizer();
	}

	@Override
	public void setLocalizer(HapiLocalizer theMessages)
	{
		delegate.setLocalizer(theMessages);
	}

	@Override
	public INarrativeGenerator getNarrativeGenerator()
	{
		return delegate.getNarrativeGenerator();
	}

	@Override
	public FhirContext setNarrativeGenerator(INarrativeGenerator theNarrativeGenerator)
	{
		return delegate.setNarrativeGenerator(theNarrativeGenerator);
	}

	@Override
	public ParserOptions getParserOptions()
	{
		return delegate.getParserOptions();
	}

	@Override
	public void setParserOptions(ParserOptions theParserOptions)
	{
		delegate.setParserOptions(theParserOptions);
	}

	@Override
	public Set<PerformanceOptionsEnum> getPerformanceOptions()
	{
		return delegate.getPerformanceOptions();
	}

	@Override
	public void setPerformanceOptions(Collection<PerformanceOptionsEnum> theOptions)
	{
		delegate.setPerformanceOptions(theOptions);
	}

	@Override
	public void setPerformanceOptions(PerformanceOptionsEnum... thePerformanceOptions)
	{
		delegate.setPerformanceOptions(thePerformanceOptions);
	}

	@Override
	public RuntimeResourceDefinition getResourceDefinition(Class<? extends IBaseResource> theResourceType)
	{
		return delegate.getResourceDefinition(theResourceType);
	}

	@Override
	public RuntimeResourceDefinition getResourceDefinition(FhirVersionEnum theVersion, String theResourceName)
	{
		return delegate.getResourceDefinition(theVersion, theResourceName);
	}

	@Override
	public RuntimeResourceDefinition getResourceDefinition(IBaseResource theResource)
	{
		return delegate.getResourceDefinition(theResource);
	}

	@Override
	public String getResourceType(Class<? extends IBaseResource> theResourceType)
	{
		return delegate.getResourceType(theResourceType);
	}

	@Override
	public String getResourceType(IBaseResource theResource)
	{
		return delegate.getResourceType(theResource);
	}

	@Override
	public String getResourceType(String theResourceName) throws DataFormatException
	{
		return delegate.getResourceType(theResourceName);
	}

	@Override
	public RuntimeResourceDefinition getResourceDefinition(String theResourceName) throws DataFormatException
	{
		return delegate.getResourceDefinition(theResourceName);
	}

	@Override
	public RuntimeResourceDefinition getResourceDefinitionById(String theId)
	{
		return delegate.getResourceDefinitionById(theId);
	}

	@Override
	public Collection<RuntimeResourceDefinition> getResourceDefinitionsWithExplicitId()
	{
		return delegate.getResourceDefinitionsWithExplicitId();
	}

	@Override
	public Set<String> getResourceTypes()
	{
		return delegate.getResourceTypes();
	}

	@Override
	public IRestfulClientFactory getRestfulClientFactory()
	{
		return restfulClientFactory;
	}

	@Override
	public void setRestfulClientFactory(IRestfulClientFactory restfulClientFactory)
	{
		this.restfulClientFactory = restfulClientFactory;
	}

	@Override
	public RuntimeChildUndeclaredExtensionDefinition getRuntimeChildUndeclaredExtensionDefinition()
	{
		return delegate.getRuntimeChildUndeclaredExtensionDefinition();
	}

	@Override
	public IValidationSupport getValidationSupport()
	{
		return delegate.getValidationSupport();
	}

	@Override
	public void setValidationSupport(IValidationSupport theValidationSupport)
	{
		delegate.setValidationSupport(theValidationSupport);
	}

	@Override
	public IFhirVersion getVersion()
	{
		return delegate.getVersion();
	}

	@Override
	public boolean hasDefaultTypeForProfile()
	{
		return delegate.hasDefaultTypeForProfile();
	}

	@Override
	public boolean isFormatXmlSupported()
	{
		return delegate.isFormatXmlSupported();
	}

	@Override
	public boolean isFormatJsonSupported()
	{
		return delegate.isFormatJsonSupported();
	}

	@Override
	public boolean isFormatNDJsonSupported()
	{
		return delegate.isFormatNDJsonSupported();
	}

	@Override
	public boolean isFormatRdfSupported()
	{
		return delegate.isFormatRdfSupported();
	}

	@Override
	public IVersionSpecificBundleFactory newBundleFactory()
	{
		return delegate.newBundleFactory();
	}

	@Override
	@SuppressWarnings("deprecation")
	public IFhirPath newFluentPath()
	{
		return delegate.newFluentPath();
	}

	@Override
	public IFhirPath newFhirPath()
	{
		return delegate.newFhirPath();
	}

	@Override
	public IParser newJsonParser()
	{
		return delegate.newJsonParser();
	}

	@Override
	public IParser newNDJsonParser()
	{
		return delegate.newNDJsonParser();
	}

	@Override
	public IParser newRDFParser()
	{
		return delegate.newRDFParser();
	}

	@Override
	public <T extends IRestfulClient> T newRestfulClient(Class<T> theClientType, String theServerBase)
	{
		return delegate.newRestfulClient(theClientType, theServerBase);
	}

	@Override
	public IGenericClient newRestfulGenericClient(String theServerBase)
	{
		return delegate.newRestfulGenericClient(theServerBase);
	}

	@Override
	public FhirTerser newTerser()
	{
		return delegate.newTerser();
	}

	@Override
	public FhirValidator newValidator()
	{
		return delegate.newValidator();
	}

	@Override
	public ViewGenerator newViewGenerator()
	{
		return delegate.newViewGenerator();
	}

	@Override
	public IParser newXmlParser()
	{
		return delegate.newXmlParser();
	}

	@Override
	public void registerCustomType(Class<? extends IBase> theType)
	{
		delegate.registerCustomType(theType);
	}

	@Override
	public void registerCustomTypes(Collection<Class<? extends IBase>> theTypes)
	{
		delegate.registerCustomTypes(theTypes);
	}

	@Override
	public void setDefaultTypeForProfile(String theProfile, Class<? extends IBaseResource> theClass)
	{
		delegate.setDefaultTypeForProfile(theProfile, theClass);
	}

	@Override
	public FhirContext setParserErrorHandler(IParserErrorHandler theParserErrorHandler)
	{
		return delegate.setParserErrorHandler(theParserErrorHandler);
	}

	@Override
	public FhirContext setFhirValidatorFactory(IFhirValidatorFactory theFhirValidatorFactory)
	{
		return delegate.setFhirValidatorFactory(theFhirValidatorFactory);
	}

	@Override
	public String toString()
	{
		return delegate.toString();
	}

	@Override
	@SuppressWarnings("removal")
	public IPrimitiveType<Boolean> getPrimitiveBoolean(Boolean theValue)
	{
		return delegate.getPrimitiveBoolean(theValue);
	}

	@Override
	public IPrimitiveType<Boolean> newPrimitiveBoolean(Boolean theValue)
	{
		return delegate.newPrimitiveBoolean(theValue);
	}

	@Override
	public IPrimitiveType<String> newPrimitiveString(String theValue)
	{
		return delegate.newPrimitiveString(theValue);
	}
}
