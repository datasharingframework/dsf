/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
