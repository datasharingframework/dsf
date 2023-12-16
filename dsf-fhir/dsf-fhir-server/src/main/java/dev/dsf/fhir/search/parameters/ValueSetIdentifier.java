package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the value set")
public class ValueSetIdentifier extends AbstractIdentifierParameter<ValueSet>
{
	private static final String RESOURCE_COLUMN = "value_set";

	public ValueSetIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof ValueSet))
			return false;

		ValueSet v = (ValueSet) resource;

		return identifierMatches(v.getIdentifier());
	}
}
