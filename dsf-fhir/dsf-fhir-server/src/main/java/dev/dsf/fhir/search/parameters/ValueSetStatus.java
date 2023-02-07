package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStatusParameter;

@SearchParameterDefinition(name = ValueSetStatus.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ValueSet-status", type = SearchParamType.TOKEN, documentation = "The current status of the value set")
public class ValueSetStatus extends AbstractStatusParameter<ValueSet>
{
	public ValueSetStatus()
	{
		super("value_set", ValueSet.class);
	}
}
