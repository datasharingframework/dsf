package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = CodeSystemDate.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-date", type = SearchParamType.DATE, documentation = "The code system publication date")
public class CodeSystemDate extends AbstractDateTimeParameter<CodeSystem>
{
	public static final String PARAMETER_NAME = "date";

	public CodeSystemDate()
	{
		super(CodeSystem.class, PARAMETER_NAME, "code_system->>'date'",
				fromDateTime(CodeSystem::hasDateElement, CodeSystem::getDateElement));
	}
}
