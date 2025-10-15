package dev.dsf.fhir.search.filter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;

public class QuestionnaireResponseIdentityFilter extends AbstractIdentityFilter
{
	private static final FhirServerRole SEARCH_ROLE = FhirServerRoleImpl.search(ResourceType.QuestionnaireResponse);
	private static final FhirServerRole READ_ROLE = FhirServerRoleImpl.read(ResourceType.QuestionnaireResponse);

	private final FhirServerRole operationRole;

	public QuestionnaireResponseIdentityFilter(Identity identity)
	{
		this(identity, SEARCH_ROLE);
	}

	public QuestionnaireResponseIdentityFilter(Identity identity, FhirServerRole operationRole)
	{
		super(identity, null, null);

		this.operationRole = operationRole;
	}

	@Override
	public String getFilterQuery()
	{
		// read allowed for local users
		if (identity.isLocalIdentity() && identity.hasDsfRole(operationRole) && identity.hasDsfRole(READ_ROLE))
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
