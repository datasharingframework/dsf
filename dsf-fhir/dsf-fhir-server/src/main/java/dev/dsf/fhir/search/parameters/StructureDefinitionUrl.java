package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractUrlAndVersionParameter;

@SearchParameterDefinition(name = StructureDefinitionUrl.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/StructureDefinition-url", type = SearchParamType.URI, documentation = "The uri that identifies the structure definition")
public class StructureDefinitionUrl extends AbstractUrlAndVersionParameter<StructureDefinition>
{
	public static final String RESOURCE_COLUMN = "structure_definition";

	public StructureDefinitionUrl()
	{
		this(RESOURCE_COLUMN);
	}

	public StructureDefinitionUrl(String resourceColumn)
	{
		super(resourceColumn);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof StructureDefinition;
	}
}
