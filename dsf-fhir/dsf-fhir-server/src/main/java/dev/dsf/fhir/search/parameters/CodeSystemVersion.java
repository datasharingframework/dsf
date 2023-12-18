package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/CodeSystem-version", type = SearchParamType.TOKEN, documentation = "The business version of the code system")
public class CodeSystemVersion extends AbstractVersionParameter<CodeSystem>
{
	public CodeSystemVersion()
	{
		super(CodeSystem.class, "code_system");
	}
}
