package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Endpoint-name", type = SearchParamType.STRING, documentation = "A name that this endpoint can be identified by")
public class EndpointName extends AbstractNameParameter<Endpoint>
{
	public EndpointName()
	{
		super(Endpoint.class, "endpoint", Endpoint::hasName, Endpoint::getName);
	}
}
