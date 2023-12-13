package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/StructureDefinition-version", type = SearchParamType.TOKEN, documentation = "The business version of the structure definition")
public class StructureDefinitionVersion extends AbstractVersionParameter<StructureDefinition>
{
	private static final String RESOURCE_COLUMN = "structure_definition";

	public StructureDefinitionVersion()
	{
		this(RESOURCE_COLUMN);
	}

	public StructureDefinitionVersion(String resourceColumn)
	{
		super(resourceColumn);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof StructureDefinition;
	}
}
