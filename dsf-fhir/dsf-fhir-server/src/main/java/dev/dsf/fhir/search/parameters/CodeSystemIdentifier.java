package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the code system")
public class CodeSystemIdentifier extends AbstractIdentifierParameter<CodeSystem>
{
	public CodeSystemIdentifier()
	{
		super(CodeSystem.class, "code_system", listMatcher(CodeSystem::hasIdentifier, CodeSystem::getIdentifier));
	}
}
