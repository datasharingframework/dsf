package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Task;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Task-identifier", type = SearchParamType.TOKEN, documentation = "Search for a task instance by its business identifier")
public class TaskIdentifier extends AbstractIdentifierParameter<Task>
{
	public TaskIdentifier()
	{
		super(Task.class, "task", listMatcher(Task::hasIdentifier, Task::getIdentifier));
	}
}
