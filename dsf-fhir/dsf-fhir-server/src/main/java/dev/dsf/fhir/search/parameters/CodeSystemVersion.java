package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/CodeSystem-version", type = SearchParamType.TOKEN, documentation = "The business version of the code system")
public class CodeSystemVersion extends AbstractVersionParameter<CodeSystem>
{
	private static final String RESOURCE_COLUMN = "code_system";

	public CodeSystemVersion()
	{
		this(RESOURCE_COLUMN);
	}

	public CodeSystemVersion(String resourceColumn)
	{
		super(resourceColumn);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof CodeSystem;
	}
}
