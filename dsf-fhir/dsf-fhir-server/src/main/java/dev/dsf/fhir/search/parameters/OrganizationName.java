package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameOrAliasParameter;

@SearchParameterDefinition(name = AbstractNameOrAliasParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Organization-name", type = SearchParamType.STRING, documentation = "A portion of the organization's name or alias")
public class OrganizationName extends AbstractNameOrAliasParameter<Organization>
{
	public OrganizationName()
	{
		super(Organization.class, "organization", Organization::hasName, Organization::getName, Organization::hasAlias,
				Organization::getAlias);
	}
}
