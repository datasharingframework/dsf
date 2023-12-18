package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Measure;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractUrlAndVersionParameter;

@SearchParameterDefinition(name = AbstractUrlAndVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Measure-url", type = SearchParamType.URI, documentation = "The uri that identifies the measure")
public class MeasureUrl extends AbstractUrlAndVersionParameter<Measure>
{
	public MeasureUrl()
	{
		super(Measure.class, "measure");
	}
}
