package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Organization-identifier", type = SearchParamType.TOKEN, documentation = "Any identifier for the organization (not the accreditation issuer's identifier)")
public class OrganizationIdentifier extends AbstractIdentifierParameter<Organization>
{
	public OrganizationIdentifier()
	{
		super(Organization.class, "organization",
				listMatcher(Organization::hasIdentifier, Organization::getIdentifier));
	}
}
