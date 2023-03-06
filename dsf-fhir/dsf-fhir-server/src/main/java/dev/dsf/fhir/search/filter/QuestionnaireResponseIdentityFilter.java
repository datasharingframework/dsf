package dev.dsf.fhir.search.filter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;

public class QuestionnaireResponseIdentityFilter extends AbstractIdentityFilter
{
	public QuestionnaireResponseIdentityFilter(Identity identity)
	{
		super(identity, null, null);
	}

	@Override
	public String getFilterQuery()
	{
		// read allowed for local users
		if (identity.isLocalIdentity() && identity.hasRole(FhirServerRole.READ))
			return "";

		// read not allowed for non local users
		else
			return "FALSE";
	}

	@Override
	public int getSqlParameterCount()
	{
		// no parameters
		return 0;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement)
			throws SQLException
	{
		// no parameters to modify
	}
}