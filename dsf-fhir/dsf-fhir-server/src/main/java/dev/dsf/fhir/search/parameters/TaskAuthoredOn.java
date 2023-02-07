package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Task;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = TaskAuthoredOn.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Task-authored-on", type = SearchParamType.DATE, documentation = "Search by creation date")
public class TaskAuthoredOn extends AbstractDateTimeParameter<Task>
{
	public static final String PARAMETER_NAME = "authored-on";

	public TaskAuthoredOn()
	{
		super(PARAMETER_NAME, "task->>'authoredOn'");
	}
}
