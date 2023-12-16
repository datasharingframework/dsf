package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the structure definition")
public class StructureDefinitionIdentifier extends AbstractIdentifierParameter<StructureDefinition>
{
	private static final String RESOURCE_COLUMN = "structure_definition";

	public StructureDefinitionIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	public StructureDefinitionIdentifier(String resourceColumn)
	{
		super(resourceColumn);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof StructureDefinition))
			return false;

		StructureDefinition s = (StructureDefinition) resource;

		return identifierMatches(s.getIdentifier());
	}
}
