package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ActivityDefinition-version", type = SearchParamType.TOKEN, documentation = "The business version of the activity definition")
public class ActivityDefinitionVersion extends AbstractVersionParameter<ActivityDefinition>
{
	public ActivityDefinitionVersion()
	{
		super(ActivityDefinition.class, "activity_definition");
	}
}
