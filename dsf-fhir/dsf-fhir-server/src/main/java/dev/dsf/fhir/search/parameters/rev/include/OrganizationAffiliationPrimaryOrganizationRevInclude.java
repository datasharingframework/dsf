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

import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.IncludeParts;

@IncludeParameterDefinition(resourceType = OrganizationAffiliation.class, parameterName = "primary-organization", targetResourceTypes = Organization.class)
public class OrganizationAffiliationPrimaryOrganizationRevInclude extends AbstractRevIncludeParameter
{
	public static final String RESOURCE_TYPE_NAME = "OrganizationAffiliation";
	public static final String PARAMETER_NAME = "primary-organization";
	public static final String TARGET_RESOURCE_TYPE_NAME = "Organization";

	public static List<String> getRevIncludeParameterValues()
	{
		return List.of(RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME,
				RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + TARGET_RESOURCE_TYPE_NAME);
	}

	@Override
	protected String getRevIncludeSql(IncludeParts includeParts)
	{
		return "(SELECT jsonb_agg(organization_affiliation) FROM current_organization_affiliations WHERE organization_affiliation->'organization' @> concat('{\"reference\": \"Organization/', organization->>'id', '\"}')::jsonb) AS organization_affiliations";
	}

	@Override
	protected void modifyRevIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for organizations
	}
}
