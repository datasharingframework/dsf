package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ActivityDefinition-version", type = SearchParamType.TOKEN, documentation = "The business version of the activity definition")
public class ActivityDefinitionVersion extends AbstractVersionParameter<ActivityDefinition>
{
	private static final String RESOURCE_COLUMN = "activity_definition";

	public ActivityDefinitionVersion()
	{
		this(RESOURCE_COLUMN);
	}

	public ActivityDefinitionVersion(String resourceColumn)
	{
		super(resourceColumn);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof ActivityDefinition;
	}
}
