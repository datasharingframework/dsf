package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ActivityDefinition-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the activity definition")
public class ActivityDefinitionIdentifier extends AbstractIdentifierParameter<ActivityDefinition>
{
	private static final String RESOURCE_COLUMN = "activity_definition";

	public ActivityDefinitionIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof ActivityDefinition))
			return false;

		ActivityDefinition e = (ActivityDefinition) resource;

		return identifierMatches(e.getIdentifier());
	}
}
