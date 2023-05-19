package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStatusParameter;

@SearchParameterDefinition(name = CodeSystemStatus.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/CodeSystem-status", type = SearchParamType.TOKEN, documentation = "The current status of the code system")
public class CodeSystemStatus extends AbstractStatusParameter<CodeSystem>
{
	public CodeSystemStatus()
	{
		super("code_system", CodeSystem.class);
	}
}
