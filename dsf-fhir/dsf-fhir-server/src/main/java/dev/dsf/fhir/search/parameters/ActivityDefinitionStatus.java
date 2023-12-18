package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStatusParameter;

@SearchParameterDefinition(name = AbstractStatusParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ActivityDefinition-status", type = SearchParamType.TOKEN, documentation = "The current status of the activity definition")
public class ActivityDefinitionStatus extends AbstractStatusParameter<ActivityDefinition>
{
	public ActivityDefinitionStatus()
	{
		super(ActivityDefinition.class, "activity_definition");
	}
}
