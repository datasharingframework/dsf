package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.PractitionerRole;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractActiveParameter;

@SearchParameterDefinition(name = AbstractActiveParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/PractitionerRole-active", type = SearchParamType.TOKEN, documentation = "Whether this practitioner role record is in active use [true|false]")
public class PractitionerRoleActive extends AbstractActiveParameter<PractitionerRole>
{
	public PractitionerRoleActive()
	{
		super(PractitionerRole.class, "practitioner_role", PractitionerRole::hasActive, PractitionerRole::getActive);
	}
}
