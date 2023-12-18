package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Organization;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractActiveParameter;

@SearchParameterDefinition(name = AbstractActiveParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Organization-active", type = SearchParamType.TOKEN, documentation = "Is the Organization record active [true|false]")
public class OrganizationActive extends AbstractActiveParameter<Organization>
{
	public OrganizationActive()
	{
		super(Organization.class, "organization", Organization::hasActive, Organization::getActive);
	}
}
