package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ValueSet-version", type = SearchParamType.TOKEN, documentation = "The business version of the value set")
public class ValueSetVersion extends AbstractVersionParameter<ValueSet>
{
	public ValueSetVersion()
	{
		super(ValueSet.class, "value_set");
	}
}
