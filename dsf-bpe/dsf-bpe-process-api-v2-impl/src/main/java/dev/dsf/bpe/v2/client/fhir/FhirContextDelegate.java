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

	public int hashCode()
	{
		return delegate.hashCode();
	}

	public boolean equals(Object obj)
	{
		return delegate.equals(obj);
	}

	public AddProfileTagEnum getAddProfileTagWhenEncoding()
	{
		return delegate.getAddProfileTagWhenEncoding();
	}

	public void setAddProfileTagWhenEncoding(AddProfileTagEnum theAddProfileTagWhenEncoding)
	{
		delegate.setAddProfileTagWhenEncoding(theAddProfileTagWhenEncoding);
	}

	public Class<? extends IBaseResource> getDefaultTypeForProfile(String theProfile)
	{
		return delegate.getDefaultTypeForProfile(theProfile);
	}

	public BaseRuntimeElementDefinition<?> getElementDefinition(Class<? extends IBase> theElementType)
	{
		return delegate.getElementDefinition(theElementType);
	}

	public BaseRuntimeElementDefinition<?> getElementDefinition(String theElementName)
	{
		return delegate.getElementDefinition(theElementName);
	}

	public Collection<BaseRuntimeElementDefinition<?>> getElementDefinitions()
	{
		return delegate.getElementDefinitions();
	}

	public HapiLocalizer getLocalizer()
	{
		return delegate.getLocalizer();
	}

	public void setLocalizer(HapiLocalizer theMessages)
	{
		delegate.setLocalizer(theMessages);
	}

	public INarrativeGenerator getNarrativeGenerator()
	{
		return delegate.getNarrativeGenerator();
	}

	public FhirContext setNarrativeGenerator(INarrativeGenerator theNarrativeGenerator)
	{
		return delegate.setNarrativeGenerator(theNarrativeGenerator);
	}

	public ParserOptions getParserOptions()
	{
		return delegate.getParserOptions();
	}

	public void setParserOptions(ParserOptions theParserOptions)
	{
		delegate.setParserOptions(theParserOptions);
	}

	public Set<PerformanceOptionsEnum> getPerformanceOptions()
	{
		return delegate.getPerformanceOptions();
	}

	public void setPerformanceOptions(Collection<PerformanceOptionsEnum> theOptions)
	{
		delegate.setPerformanceOptions(theOptions);
	}

	public void setPerformanceOptions(PerformanceOptionsEnum... thePerformanceOptions)
	{
		delegate.setPerformanceOptions(thePerformanceOptions);
	}

	public RuntimeResourceDefinition getResourceDefinition(Class<? extends IBaseResource> theResourceType)
	{
		return delegate.getResourceDefinition(theResourceType);
	}

	public RuntimeResourceDefinition getResourceDefinition(FhirVersionEnum theVersion, String theResourceName)
	{
		return delegate.getResourceDefinition(theVersion, theResourceName);
	}

	public RuntimeResourceDefinition getResourceDefinition(IBaseResource theResource)
	{
		return delegate.getResourceDefinition(theResource);
	}

	public String getResourceType(Class<? extends IBaseResource> theResourceType)
	{
		return delegate.getResourceType(theResourceType);
	}

	public String getResourceType(IBaseResource theResource)
	{
		return delegate.getResourceType(theResource);
	}

	public String getResourceType(String theResourceName) throws DataFormatException
	{
		return delegate.getResourceType(theResourceName);
	}

	public RuntimeResourceDefinition getResourceDefinition(String theResourceName) throws DataFormatException
	{
		return delegate.getResourceDefinition(theResourceName);
	}

	public RuntimeResourceDefinition getResourceDefinitionById(String theId)
	{
		return delegate.getResourceDefinitionById(theId);
	}

	public Collection<RuntimeResourceDefinition> getResourceDefinitionsWithExplicitId()
	{
		return delegate.getResourceDefinitionsWithExplicitId();
	}

	public Set<String> getResourceTypes()
	{
		return delegate.getResourceTypes();
	}

	public IRestfulClientFactory getRestfulClientFactory()
	{
		return restfulClientFactory;
	}

	public void setRestfulClientFactory(IRestfulClientFactory restfulClientFactory)
	{
		this.restfulClientFactory = restfulClientFactory;
	}

	public RuntimeChildUndeclaredExtensionDefinition getRuntimeChildUndeclaredExtensionDefinition()
	{
		return delegate.getRuntimeChildUndeclaredExtensionDefinition();
	}

	public IValidationSupport getValidationSupport()
	{
		return delegate.getValidationSupport();
	}

	public void setValidationSupport(IValidationSupport theValidationSupport)
	{
		delegate.setValidationSupport(theValidationSupport);
	}

	public IFhirVersion getVersion()
	{
		return delegate.getVersion();
	}

	public boolean hasDefaultTypeForProfile()
	{
		return delegate.hasDefaultTypeForProfile();
	}

	public boolean isFormatXmlSupported()
	{
		return delegate.isFormatXmlSupported();
	}

	public boolean isFormatJsonSupported()
	{
		return delegate.isFormatJsonSupported();
	}

	public boolean isFormatNDJsonSupported()
	{
		return delegate.isFormatNDJsonSupported();
	}

	public boolean isFormatRdfSupported()
	{
		return delegate.isFormatRdfSupported();
	}

	public IVersionSpecificBundleFactory newBundleFactory()
	{
		return delegate.newBundleFactory();
	}

	@SuppressWarnings("deprecation")
	public IFhirPath newFluentPath()
	{
		return delegate.newFluentPath();
	}

	public IFhirPath newFhirPath()
	{
		return delegate.newFhirPath();
	}

	public IParser newJsonParser()
	{
		return delegate.newJsonParser();
	}

	public IParser newNDJsonParser()
	{
		return delegate.newNDJsonParser();
	}

	public IParser newRDFParser()
	{
		return delegate.newRDFParser();
	}

	public <T extends IRestfulClient> T newRestfulClient(Class<T> theClientType, String theServerBase)
	{
		return delegate.newRestfulClient(theClientType, theServerBase);
	}

	public IGenericClient newRestfulGenericClient(String theServerBase)
	{
		return delegate.newRestfulGenericClient(theServerBase);
	}

	public FhirTerser newTerser()
	{
		return delegate.newTerser();
	}

	public FhirValidator newValidator()
	{
		return delegate.newValidator();
	}

	public ViewGenerator newViewGenerator()
	{
		return delegate.newViewGenerator();
	}

	public IParser newXmlParser()
	{
		return delegate.newXmlParser();
	}

	public void registerCustomType(Class<? extends IBase> theType)
	{
		delegate.registerCustomType(theType);
	}

	public void registerCustomTypes(Collection<Class<? extends IBase>> theTypes)
	{
		delegate.registerCustomTypes(theTypes);
	}

	public void setDefaultTypeForProfile(String theProfile, Class<? extends IBaseResource> theClass)
	{
		delegate.setDefaultTypeForProfile(theProfile, theClass);
	}

	public FhirContext setParserErrorHandler(IParserErrorHandler theParserErrorHandler)
	{
		return delegate.setParserErrorHandler(theParserErrorHandler);
	}

	public FhirContext setFhirValidatorFactory(IFhirValidatorFactory theFhirValidatorFactory)
	{
		return delegate.setFhirValidatorFactory(theFhirValidatorFactory);
	}

	public String toString()
	{
		return delegate.toString();
	}

	@SuppressWarnings("removal")
	public IPrimitiveType<Boolean> getPrimitiveBoolean(Boolean theValue)
	{
		return delegate.getPrimitiveBoolean(theValue);
	}

	public IPrimitiveType<Boolean> newPrimitiveBoolean(Boolean theValue)
	{
		return delegate.newPrimitiveBoolean(theValue);
	}

	public IPrimitiveType<String> newPrimitiveString(String theValue)
	{
		return delegate.newPrimitiveString(theValue);
	}
}
