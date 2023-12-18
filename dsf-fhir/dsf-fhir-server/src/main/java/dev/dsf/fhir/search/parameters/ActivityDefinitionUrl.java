package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractUrlAndVersionParameter;

@SearchParameterDefinition(name = AbstractUrlAndVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ActivityDefinition-url", type = SearchParamType.URI, documentation = "The uri that identifies the activity definition")
public class ActivityDefinitionUrl extends AbstractUrlAndVersionParameter<ActivityDefinition>
{
	public ActivityDefinitionUrl()
	{
		super(ActivityDefinition.class, "activity_definition");
	}
}
