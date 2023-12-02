package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Task-identifier", type = SearchParamType.TOKEN, documentation = "Search for a task instance by its business identifier")
public class TaskIdentifier extends AbstractIdentifierParameter<Task>
{
	private static final String RESOURCE_COLUMN = "task";

	public TaskIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof Task))
			return false;

		Task t = (Task) resource;

		return identifierMatches(t.getIdentifier());
	}
}
