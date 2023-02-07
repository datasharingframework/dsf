package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = ResourceLastUpdated.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Resource-lastUpdated", type = SearchParamType.DATE, documentation = "When the resource version last changed")
public class ResourceLastUpdated<R extends DomainResource> extends AbstractDateTimeParameter<R>
{
	public static final String PARAMETER_NAME = "_lastUpdated";

	public ResourceLastUpdated(String resourceColumn)
	{
		super(PARAMETER_NAME, resourceColumn + "->'meta'->>'lastUpdated'");
	}
}
