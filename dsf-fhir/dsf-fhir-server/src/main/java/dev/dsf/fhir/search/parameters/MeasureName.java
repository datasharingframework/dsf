package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Measure;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Measure-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the measure")
public class MeasureName extends AbstractNameParameter<Measure>
{
	public MeasureName()
	{
		super(Measure.class, "measure", Measure::hasName, Measure::getName);
	}
}
