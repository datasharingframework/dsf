package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStatusParameter;

@SearchParameterDefinition(name = AbstractStatusParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ActivityDefinition-status", type = SearchParamType.TOKEN, documentation = "The current status of the activity definition")
public class ActivityDefinitionStatus extends AbstractStatusParameter<ActivityDefinition>
{
	public static final String RESOURCE_COLUMN = "activity_definition";

	public ActivityDefinitionStatus()
	{
		super(RESOURCE_COLUMN, ActivityDefinition.class);
	}
}
