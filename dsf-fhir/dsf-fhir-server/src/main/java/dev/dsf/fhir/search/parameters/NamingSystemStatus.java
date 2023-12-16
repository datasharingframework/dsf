package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.NamingSystem;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStatusParameter;

@SearchParameterDefinition(name = AbstractStatusParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/NamingSystem-status", type = SearchParamType.TOKEN, documentation = "The current status of the naming system")
public class NamingSystemStatus extends AbstractStatusParameter<NamingSystem>
{
	private static final String RESOURCE_COLUMN = "naming_system";

	public NamingSystemStatus()
	{
		super(RESOURCE_COLUMN, NamingSystem.class);
	}
}
