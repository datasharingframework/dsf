package dev.dsf.fhir.search.filter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;

public class TaskIdentityFilter extends AbstractIdentityFilter
{
	private static final String RESOURCE_COLUMN = "task";

	private final String resourceColumn;

	public TaskIdentityFilter(Identity identity)
	{
		super(identity, null, null);

		this.resourceColumn = RESOURCE_COLUMN;
	}

	public TaskIdentityFilter(Identity identity, String resourceColumn)
	{
		super(identity, null, null);

		this.resourceColumn = resourceColumn;
	}

	@Override
	public String getFilterQuery()
	{
		if (identity.hasDsfRole(FhirServerRole.READ))
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
		return identity.hasDsfRole(FhirServerRole.READ) ? 4 : 0;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement)
			throws SQLException
	{
		if (identity.hasDsfRole(FhirServerRole.READ))
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
