package dev.dsf.fhir.search.parameters.rev.include;

import java.sql.Connection;
import java.util.List;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.IncludeParts;

@IncludeParameterDefinition(resourceType = Organization.class, parameterName = "endpoint", targetResourceTypes = Endpoint.class)
public class OrganizationEndpointRevInclude extends AbstractRevIncludeParameter
{
	public static final String RESOURCE_TYPE_NAME = "Organization";
	public static final String PARAMETER_NAME = "endpoint";
	public static final String TARGET_RESOURCE_TYPE_NAME = "Endpoint";

	public static List<String> getRevIncludeParameterValues()
	{
		return List.of(RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME,
				RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + TARGET_RESOURCE_TYPE_NAME);
	}

	@Override
	protected String getRevIncludeSql(IncludeParts includeParts)
	{
		return "(SELECT jsonb_agg(organization) FROM current_organizations WHERE organization->'endpoint' @> concat('[{\"reference\": \"Endpoint/', endpoint->>'id', '\"}]')::jsonb) AS organizations";
	}

	@Override
	protected void modifyRevIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for organizations
	}
}
