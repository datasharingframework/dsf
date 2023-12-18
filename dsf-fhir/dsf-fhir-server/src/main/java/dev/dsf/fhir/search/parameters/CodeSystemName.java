package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the code system")
public class CodeSystemName extends AbstractNameParameter<CodeSystem>
{
	public CodeSystemName()
	{
		super(CodeSystem.class, "code_system", CodeSystem::hasName, CodeSystem::getName);
	}
}
