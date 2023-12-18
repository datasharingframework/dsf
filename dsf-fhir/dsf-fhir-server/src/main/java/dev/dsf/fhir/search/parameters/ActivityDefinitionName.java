package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ActivityDefinition-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the activity definition")
public class ActivityDefinitionName extends AbstractNameParameter<ActivityDefinition>
{
	public ActivityDefinitionName()
	{
		super(ActivityDefinition.class, "activity_definition", ActivityDefinition::hasName,
				ActivityDefinition::getName);
	}
}
