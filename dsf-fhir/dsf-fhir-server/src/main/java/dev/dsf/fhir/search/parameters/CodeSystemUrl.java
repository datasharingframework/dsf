package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractUrlAndVersionParameter;

@SearchParameterDefinition(name = CodeSystemUrl.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/CodeSystem-url", type = SearchParamType.URI, documentation = "The uri that identifies the code system")
public class CodeSystemUrl extends AbstractUrlAndVersionParameter<CodeSystem>
{
	public static final String RESOURCE_COLUMN = "code_system";

	public CodeSystemUrl()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof CodeSystem;
	}
}
