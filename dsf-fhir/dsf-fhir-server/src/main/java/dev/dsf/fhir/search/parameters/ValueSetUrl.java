package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractUrlAndVersionParameter;

@SearchParameterDefinition(name = AbstractUrlAndVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ValueSet-url", type = SearchParamType.URI, documentation = "The uri that identifies the value set")
public class ValueSetUrl extends AbstractUrlAndVersionParameter<ValueSet>
{
	private static final String RESOURCE_COLUMN = "value_set";

	public ValueSetUrl()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof ValueSet;
	}
}
