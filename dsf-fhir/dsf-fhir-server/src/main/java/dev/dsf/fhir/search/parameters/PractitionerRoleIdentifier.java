package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.PractitionerRole;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/PractitionerRole-identifier", type = SearchParamType.TOKEN, documentation = "A practitioner's Identifier")
public class PractitionerRoleIdentifier extends AbstractIdentifierParameter<PractitionerRole>
{
	public PractitionerRoleIdentifier()
	{
		super(PractitionerRole.class, "practitioner_role",
				listMatcher(PractitionerRole::hasIdentifier, PractitionerRole::getIdentifier));
	}
}
