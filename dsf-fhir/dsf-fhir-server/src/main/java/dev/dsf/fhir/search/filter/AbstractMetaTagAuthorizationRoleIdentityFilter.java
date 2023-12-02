package dev.dsf.fhir.search.filter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.postgresql.util.PGobject;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;

abstract class AbstractMetaTagAuthorizationRoleIdentityFilter extends AbstractIdentityFilter
{
	AbstractMetaTagAuthorizationRoleIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}

	@Override
	public String getFilterQuery()
	{
		if (identity.isLocalIdentity() && identity.hasDsfRole(FhirServerRole.READ))
			return "(SELECT count(*) FROM read_access WHERE read_access.resource_id = " + resourceTable + "."
					+ resourceIdColumn + " AND read_access.resource_version = " + resourceTable + ".version"
					+ " AND (read_access.organization_id = ? OR read_access.access_type = 'ALL' OR read_access.access_type = 'LOCAL')) > 0";
		else if (identity.hasDsfRole(FhirServerRole.READ))
			return "(SELECT count(*) FROM read_access WHERE read_access.resource_id = " + resourceTable + "."
					+ resourceIdColumn + " AND read_access.resource_version = " + resourceTable + ".version"
					+ " AND (read_access.organization_id = ? OR read_access.access_type = 'ALL')) > 0";
		else
			return "FALSE";
	}

	@Override
	public int getSqlParameterCount()
	{
		return identity.hasDsfRole(FhirServerRole.READ) ? 1 : 0;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement)
			throws SQLException
	{
		if (identity.hasDsfRole(FhirServerRole.READ))
		{
			String usersOrganizationId = identity.getOrganization().getIdElement().getIdPart();
			statement.setObject(parameterIndex, toUuidObject(usersOrganizationId));
		}
	}

	private PGobject toUuidObject(String uuid) throws SQLException
	{
		if (uuid == null)
			return null;

		PGobject uuidObject = new PGobject();
		uuidObject.setType("UUID");
		uuidObject.setValue(uuid);
		return uuidObject;
	}
}
