package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Task;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = TaskModified.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Task-modified", type = SearchParamType.DATE, documentation = "Search by last modification date")
public class TaskModified extends AbstractDateTimeParameter<Task>
{
	public static final String PARAMETER_NAME = "modified";

	public TaskModified()
	{
		super(Task.class, PARAMETER_NAME, "task->>'lastModified'",
				fromDateTime(Task::hasLastModifiedElement, Task::getLastModifiedElement));
	}
}
