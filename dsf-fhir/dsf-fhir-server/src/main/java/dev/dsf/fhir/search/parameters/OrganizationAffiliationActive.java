package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractActiveParameter;

@SearchParameterDefinition(name = AbstractActiveParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/OrganizationAffiliation-active", type = SearchParamType.TOKEN, documentation = "Whether this organization affiliation record is in active use [true|false]")
public class OrganizationAffiliationActive extends AbstractActiveParameter<OrganizationAffiliation>
{
	public OrganizationAffiliationActive()
	{
		super(OrganizationAffiliation.class, "organization_affiliation", OrganizationAffiliation::hasActive,
				OrganizationAffiliation::getActive);
	}
}
