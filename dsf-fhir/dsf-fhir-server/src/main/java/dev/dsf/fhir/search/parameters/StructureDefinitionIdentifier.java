package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-identifier", type = SearchParamType.TOKEN, documentation = "External identifier for the structure definition")
public class StructureDefinitionIdentifier extends AbstractIdentifierParameter<StructureDefinition>
{
	public StructureDefinitionIdentifier()
	{
		this("structure_definition");
	}

	public StructureDefinitionIdentifier(String resourceColumn)
	{
		super(StructureDefinition.class, resourceColumn,
				listMatcher(StructureDefinition::hasIdentifier, StructureDefinition::getIdentifier));
	}
}
