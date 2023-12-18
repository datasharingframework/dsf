package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;
import dev.dsf.fhir.search.parameters.basic.AbstractSingleIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Bundle-identifier", type = SearchParamType.TOKEN, documentation = "Persistent identifier for the bundle")
public class BundleIdentifier extends AbstractSingleIdentifierParameter<Bundle>
{
	public BundleIdentifier()
	{
		super(Bundle.class, "bundle", singleMatcher(Bundle::hasIdentifier, Bundle::getIdentifier));
	}
}
