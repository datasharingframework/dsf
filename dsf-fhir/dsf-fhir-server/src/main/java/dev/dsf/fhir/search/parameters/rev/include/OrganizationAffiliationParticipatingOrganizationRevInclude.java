package dev.dsf.fhir.search.parameters.rev.include;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.IncludeParts;

@IncludeParameterDefinition(resourceType = OrganizationAffiliation.class, parameterName = "participating-organization", targetResourceTypes = Organization.class)
public class OrganizationAffiliationParticipatingOrganizationRevInclude extends AbstractRevIncludeParameter
{
	public static final String RESOURCE_TYPE_NAME = "OrganizationAffiliation";
	public static final String PARAMETER_NAME = "participating-organization";
	public static final String TARGET_RESOURCE_TYPE_NAME = "Organization";

	public static List<String> getRevIncludeParameterValues()
	{
		return Arrays.asList(RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME,
				RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + TARGET_RESOURCE_TYPE_NAME);
	}

	@Override
	protected String getRevIncludeSql(IncludeParts includeParts)
	{
		return "(SELECT jsonb_agg(organization_affiliation) FROM current_organization_affiliations WHERE organization_affiliation->'participatingOrganization' @> concat('{\"reference\": \"Organization/', organization->>'id', '\"}')::jsonb) AS organization_affiliations";
	}

	@Override
	protected void modifyRevIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for organizations
	}
}
