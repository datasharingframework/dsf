package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Measure;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStatusParameter;

@SearchParameterDefinition(name = AbstractStatusParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Measure-status", type = SearchParamType.TOKEN, documentation = "The current status of the measure")
public class MeasureStatus extends AbstractStatusParameter<Measure>
{
	private static final String RESOURCE_COLUMN = "measure";

	public MeasureStatus()
	{
		super(RESOURCE_COLUMN, Measure.class);
	}
}
