package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ActivityDefinition-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the activity definition")
public class ActivityDefinitionIdentifier extends AbstractIdentifierParameter<ActivityDefinition>
{
	public ActivityDefinitionIdentifier()
	{
		super(ActivityDefinition.class, "activity_definition",
				listMatcher(ActivityDefinition::hasIdentifier, ActivityDefinition::getIdentifier));
	}
}
