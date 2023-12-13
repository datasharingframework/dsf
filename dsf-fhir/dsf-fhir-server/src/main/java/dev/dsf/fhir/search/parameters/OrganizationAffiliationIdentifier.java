package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/OrganizationAffiliation-identifier", type = SearchParamType.TOKEN, documentation = "An organization affiliation's Identifier")
public class OrganizationAffiliationIdentifier extends AbstractIdentifierParameter<OrganizationAffiliation>
{
	private static final String RESOURCE_COLUMN = "organization_affiliation";

	public OrganizationAffiliationIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof OrganizationAffiliation))
			return false;

		OrganizationAffiliation o = (OrganizationAffiliation) resource;

		return identifierMatches(o.getIdentifier());
	}
}
