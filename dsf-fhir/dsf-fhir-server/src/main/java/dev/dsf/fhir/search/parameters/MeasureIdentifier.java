package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Measure;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Measure-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the measure")
public class MeasureIdentifier extends AbstractIdentifierParameter<Measure>
{
	public MeasureIdentifier()
	{
		super(Measure.class, "measure", listMatcher(Measure::hasIdentifier, Measure::getIdentifier));
	}
}
