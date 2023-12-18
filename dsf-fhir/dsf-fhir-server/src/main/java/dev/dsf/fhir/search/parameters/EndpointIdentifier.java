package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Endpoint-identifier", type = SearchParamType.TOKEN, documentation = "Identifies this endpoint across multiple systems")
public class EndpointIdentifier extends AbstractIdentifierParameter<Endpoint>
{
	public EndpointIdentifier()
	{
		super(Endpoint.class, "endpoint", listMatcher(Endpoint::hasIdentifier, Endpoint::getIdentifier));
	}
}
