package dev.dsf.fhir.search.filter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
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
			// TODO modify for requester = Practitioner or PractitionerRole
			return "(" + resourceColumn + "->'requester'->>'reference' = ? OR " + resourceColumn
					+ "->'requester'->>'reference' = ? OR " + resourceColumn
					+ "->'restriction'->'recipient' @> ?::jsonb OR " + resourceColumn
					+ "->'restriction'->'recipient' @> ?::jsonb)";
		}
		else
			return "FALSE";
	}

	@Override
	public int getSqlParameterCount()
	{
		return identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE) ? 4 : 0;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement)
			throws SQLException
	{
		if (identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE))
		{
			if (subqueryParameterIndex == 1)
				statement.setString(parameterIndex, identity.getOrganization().getIdElement().getValue());
			else if (subqueryParameterIndex == 2)
				statement.setString(parameterIndex,
						identity.getOrganization().getIdElement().toVersionless().getValue());
			else if (subqueryParameterIndex == 3)
				statement.setString(parameterIndex,
						"[{\"reference\": \"" + identity.getOrganization().getIdElement().getValue() + "\"}]");
			else if (subqueryParameterIndex == 4)
				statement.setString(parameterIndex, "[{\"reference\": \""
						+ identity.getOrganization().getIdElement().toVersionless().getValue() + "\"}]");
		}
	}
}
