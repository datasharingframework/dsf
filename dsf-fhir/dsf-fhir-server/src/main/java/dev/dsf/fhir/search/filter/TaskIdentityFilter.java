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
package dev.dsf.fhir.search.filter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;

public class TaskIdentityFilter extends AbstractIdentityFilter
{
	private static final FhirServerRole SEARCH_ROLE = FhirServerRoleImpl.search(ResourceType.Task);
	private static final FhirServerRole READ_ROLE = FhirServerRoleImpl.read(ResourceType.Task);

	private static final String RESOURCE_COLUMN = "task";

	private final String resourceColumn;
	private final FhirServerRole operationRole;

	public TaskIdentityFilter(Identity identity)
	{
		this(identity, RESOURCE_COLUMN, SEARCH_ROLE);
	}

	public TaskIdentityFilter(Identity identity, String resourceColumn, FhirServerRole operationRole)
	{
		super(identity, null, null);

		this.resourceColumn = resourceColumn;
		this.operationRole = operationRole;
	}

	@Override
	public String getFilterQuery()
	{
		if (identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE))
		{
			if (identity instanceof OrganizationIdentity)
			{
				if (identity.isLocalIdentity())
					return resourceColumn + "->'restriction'->'recipient' @> ?::jsonb";
				else
					return resourceColumn + "->'requester'->>'reference' = ?";
			}
			else if (identity instanceof PractitionerIdentity p)
			{
				if (p.hasPractionerRole("DSF_ADMIN"))
					return resourceColumn + "->'restriction'->'recipient' @> ?::jsonb";
				else if (p.getPractitionerIdentifierValue().isPresent())
				{
					return "((" + resourceColumn + "->'requester'->'identifier'->>'system' = '"
							+ PractitionerIdentity.PRACTITIONER_IDENTIFIER_SYSTEM + "' AND " + resourceColumn
							+ "->'requester'->'identifier'->>'value' = ?) OR (" + resourceColumn
							+ "->>'status' = 'draft' AND " + resourceColumn + "->'restriction'->'recipient' @> ?::jsonb"
							+ "))";
				}
				else
					return "(" + resourceColumn + "->>'status' = 'draft' AND " + resourceColumn
							+ "->'restriction'->'recipient' @> ?::jsonb" + ")";
			}
		}

		return "FALSE";
	}

	@Override
	public int getSqlParameterCount()
	{
		if (identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE))
		{
			if (identity instanceof OrganizationIdentity)
				return 1;
			else if (identity instanceof PractitionerIdentity p)
			{
				if (p.hasPractionerRole("DSF_ADMIN"))
					return 1;
				else if (p.getPractitionerIdentifierValue().isPresent())
					return 2;
				else
					return 1;
			}
		}

		return 0;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement)
			throws SQLException
	{
		if (identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE))
		{
			if (identity instanceof OrganizationIdentity)
			{
				if (identity.isLocalIdentity())
				{
					statement.setString(parameterIndex, "[{\"reference\": \""
							+ identity.getOrganization().getIdElement().toUnqualifiedVersionless().getValue() + "\"}]");
				}
				else
				{
					statement.setString(parameterIndex,
							identity.getOrganization().getIdElement().toUnqualifiedVersionless().getValue());
				}
			}
			else if (identity instanceof PractitionerIdentity p)
			{
				if (p.hasPractionerRole("DSF_ADMIN"))
				{
					statement.setString(parameterIndex, "[{\"reference\": \""
							+ identity.getOrganization().getIdElement().toUnqualifiedVersionless().getValue() + "\"}]");
				}
				else if (p.getPractitionerIdentifierValue().isPresent())
				{
					if (subqueryParameterIndex == 1)
						statement.setString(parameterIndex, p.getPractitionerIdentifierValue().get());
					else if (subqueryParameterIndex == 2)
					{
						statement.setString(parameterIndex, "[{\"reference\": \""
								+ identity.getOrganization().getIdElement().toUnqualifiedVersionless().getValue()
								+ "\"}]");
					}
				}
				else
				{
					statement.setString(parameterIndex, "[{\"reference\": \""
							+ identity.getOrganization().getIdElement().toUnqualifiedVersionless().getValue() + "\"}]");
				}
			}
		}
	}
}
