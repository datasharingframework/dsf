package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/OrganizationAffiliation-identifier", type = SearchParamType.TOKEN, documentation = "An organization affiliation's Identifier")
public class OrganizationAffiliationIdentifier extends AbstractIdentifierParameter<OrganizationAffiliation>
{
	public OrganizationAffiliationIdentifier()
	{
		super(OrganizationAffiliation.class, "organization_affiliation",
				listMatcher(OrganizationAffiliation::hasIdentifier, OrganizationAffiliation::getIdentifier));
	}
}
