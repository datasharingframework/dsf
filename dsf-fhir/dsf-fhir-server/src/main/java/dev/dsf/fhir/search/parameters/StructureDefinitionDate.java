package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.StructureDefinition;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

@SearchParameterDefinition(name = StructureDefinitionDate.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-date", type = SearchParamType.DATE, documentation = "The structure definition publication date")
public class StructureDefinitionDate extends AbstractDateTimeParameter<StructureDefinition>
{
	public static final String PARAMETER_NAME = "date";

	public StructureDefinitionDate()
	{
		this("structure_definition");
	}

	public StructureDefinitionDate(String resourceColumn)
	{
		super(StructureDefinition.class, PARAMETER_NAME, resourceColumn + "->>'date'",
				fromDateTime(StructureDefinition::hasDateElement, StructureDefinition::getDateElement));
	}
}
