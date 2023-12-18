package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Location;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Location-identifier", type = SearchParamType.TOKEN, documentation = "An identifier for the location")
public class LocationIdentifier extends AbstractIdentifierParameter<Location>
{
	public LocationIdentifier()
	{
		super(Location.class, "location", listMatcher(Location::hasIdentifier, Location::getIdentifier));
	}
}
