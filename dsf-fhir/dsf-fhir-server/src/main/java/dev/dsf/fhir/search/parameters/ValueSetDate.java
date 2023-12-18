package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = ValueSetDate.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-date", type = SearchParamType.DATE, documentation = "The value set publication date")
public class ValueSetDate extends AbstractDateTimeParameter<ValueSet>
{
	public static final String PARAMETER_NAME = "date";

	public ValueSetDate()
	{
		super(ValueSet.class, PARAMETER_NAME, "value_set->>'date'",
				fromDateTime(ValueSet::hasDateElement, ValueSet::getDateElement));
	}
}
