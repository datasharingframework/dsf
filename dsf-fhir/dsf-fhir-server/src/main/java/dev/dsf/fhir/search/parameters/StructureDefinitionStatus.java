package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStatusParameter;

@SearchParameterDefinition(name = StructureDefinitionStatus.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/StructureDefinition-status", type = SearchParamType.TOKEN, documentation = "The current status of the structure definition")
public class StructureDefinitionStatus extends AbstractStatusParameter<StructureDefinition>
{
	public static final String RESOURCE_COLUMN = "structure_definition";

	public StructureDefinitionStatus()
	{
		this(RESOURCE_COLUMN);
	}

	public StructureDefinitionStatus(String resourceColumn)
	{
		super(resourceColumn, StructureDefinition.class);
	}
}
