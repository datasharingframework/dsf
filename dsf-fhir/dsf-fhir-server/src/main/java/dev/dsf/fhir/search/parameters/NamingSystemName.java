package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.NamingSystem;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/conformance-name", type = SearchParamType.STRING, documentation = "Computationally friendly name of the naming system")
public class NamingSystemName extends AbstractNameParameter<NamingSystem>
{
	public NamingSystemName()
	{
		super(NamingSystem.class, "naming_system", NamingSystem::hasName, NamingSystem::getName);
	}
}
