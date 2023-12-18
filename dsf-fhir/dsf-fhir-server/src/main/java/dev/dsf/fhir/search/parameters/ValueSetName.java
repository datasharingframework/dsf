package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the value set")
public class ValueSetName extends AbstractNameParameter<ValueSet>
{
	public ValueSetName()
	{
		super(ValueSet.class, "value_set", ValueSet::hasName, ValueSet::getName);
	}
}
