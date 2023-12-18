package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Measure;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Measure-version", type = SearchParamType.TOKEN, documentation = "The business version of the measure")
public class MeasureVersion extends AbstractVersionParameter<Measure>
{
	public MeasureVersion()
	{
		super(Measure.class, "measure");
	}
}
