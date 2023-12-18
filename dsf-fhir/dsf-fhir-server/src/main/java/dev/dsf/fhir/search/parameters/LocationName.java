package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Location;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameOrAliasParameter;

@SearchParameterDefinition(name = AbstractNameOrAliasParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Location-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the library")
public class LocationName extends AbstractNameOrAliasParameter<Location>
{
	public LocationName()
	{
		super(Location.class, "location", Location::hasName, Location::getName, Location::hasAlias, Location::getAlias);
	}
}
