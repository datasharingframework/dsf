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
import java.util.Set;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;

public class QuestionnaireResponseIdentityFilter extends AbstractIdentityFilter
{
	private static final FhirServerRole SEARCH_ROLE = FhirServerRoleImpl.search(ResourceType.QuestionnaireResponse);
	private static final FhirServerRole READ_ROLE = FhirServerRoleImpl.read(ResourceType.QuestionnaireResponse);

	private static final String RESOURCE_COLUMN = "questionnaire_response";

	private final String resourceColumn;
	private final FhirServerRole operationRole;

	public QuestionnaireResponseIdentityFilter(Identity identity)
	{
		this(identity, RESOURCE_COLUMN, SEARCH_ROLE);
	}

	public QuestionnaireResponseIdentityFilter(Identity identity, String resourceColumn, FhirServerRole operationRole)
	{
		super(identity, null, null);

		this.resourceColumn = resourceColumn;
		this.operationRole = operationRole;
	}

	@Override
	public String getFilterQuery()
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE))
		{
			if (identity instanceof OrganizationIdentity
					|| (identity instanceof PractitionerIdentity p && p.hasPractionerRole("DSF_ADMIN")))
				return "";
			else if (identity instanceof PractitionerIdentity p && p.getPractitionerIdentifierValue().isPresent())
				return "EXISTS (SELECT 1 FROM jsonb_array_elements(" + resourceColumn + "->'extension') AS authExt "
						+ "WHERE authExt->>'url' = 'http://dsf.dev/fhir/StructureDefinition/extension-questionnaire-authorization' "
						+ "AND EXISTS (SELECT 1 FROM jsonb_array_elements(authExt->'extension') AS ext "
						+ "WHERE ((ext->>'url' = 'practitioner' AND ext->'valueIdentifier'->>'value' = ?) "
						+ "OR (ext->>'url' = 'practitioner-role' AND ("
						+ "SELECT COUNT(*) FROM jsonb_array_elements(?::jsonb) AS allowed_roles "
						+ "WHERE allowed_roles->>'system' = ext->'valueCoding'->>'system' AND allowed_roles->>'code' = ext->'valueCoding'->>'code'"
						+ ") > 0))))";
		}

		return "FALSE";
	}

	@Override
	public int getSqlParameterCount()
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE)
				&& identity instanceof PractitionerIdentity p && !p.hasPractionerRole("DSF_ADMIN")
				&& p.getPractitionerIdentifierValue().isPresent())
			return 2;
		else
			return 0;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement)
			throws SQLException
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE)
				&& identity instanceof PractitionerIdentity p && !p.hasPractionerRole("DSF_ADMIN")
				&& p.getPractitionerIdentifierValue().isPresent())
		{
			if (subqueryParameterIndex == 1)
				statement.setString(parameterIndex, p.getPractitionerIdentifierValue().get());
			else if (subqueryParameterIndex == 2)
				statement.setString(parameterIndex, toJson(p.getPractionerRoles()));
		}
	}

	private String toJson(Set<Coding> roles)
	{
		return roles.stream().map(c -> "{\"system\":\"%s\",\"code\":\"%s\"}".formatted(c.getSystem(), c.getCode()))
				.collect(Collectors.joining(",", "[", "]"));
	}
}
