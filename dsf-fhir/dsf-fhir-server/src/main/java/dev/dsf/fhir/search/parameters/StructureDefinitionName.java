package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the structure definition")
public class StructureDefinitionName extends AbstractNameParameter<StructureDefinition>
{
	public StructureDefinitionName()
	{
		this("structure_definition");
	}

	public StructureDefinitionName(String resourceColumn)
	{
		super(StructureDefinition.class, resourceColumn, StructureDefinition::hasName, StructureDefinition::getName);
	}
}
