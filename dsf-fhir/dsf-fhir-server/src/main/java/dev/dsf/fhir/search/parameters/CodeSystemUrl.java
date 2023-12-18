package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractUrlAndVersionParameter;

@SearchParameterDefinition(name = AbstractUrlAndVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/CodeSystem-url", type = SearchParamType.URI, documentation = "The uri that identifies the code system")
public class CodeSystemUrl extends AbstractUrlAndVersionParameter<CodeSystem>
{
	public CodeSystemUrl()
	{
		super(CodeSystem.class, "code_system");
	}
}
