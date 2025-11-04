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
package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.sql.DataSource;

import org.postgresql.util.PGobject;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.parser.DataFormatException;
import dev.dsf.fhir.dao.ReadAccessDao;

public class ReadAccessDaoJdbc implements ReadAccessDao, InitializingBean
{
	private final DataSource dataSource;

	public ReadAccessDaoJdbc(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dataSource, "dataSource");
	}

	@Override
	public List<String> getAccessTypes(Connection connection, UUID resourceId, long version, boolean localIdentity,
			UUID organizationId) throws SQLException
	{
		Objects.requireNonNull(connection, "connection");
		Objects.requireNonNull(resourceId, "resourceId");
		if (version <= 0)
			throw new IllegalArgumentException("version <= 0");
		Objects.requireNonNull(organizationId, "organizationId");

		try (PreparedStatement statement = connection.prepareStatement(getReadAllowedQuery(localIdentity)))
		{
			statement.setObject(1, uuidToPgObject(resourceId));
			statement.setLong(2, version);
			statement.setObject(3, uuidToPgObject(organizationId));

			try (ResultSet result = statement.executeQuery())
			{
				List<String> accessTypes = new ArrayList<>();
				while (result.next())
					accessTypes.add(result.getString(1));
				return accessTypes;
			}
		}
	}

	private String getReadAllowedQuery(boolean localIdentity)
	{
		if (localIdentity)
			return "SELECT DISTINCT access_type FROM read_access WHERE resource_id = ? AND resource_version = ? AND (access_type = 'ALL' OR access_type = 'LOCAL' OR organization_id = ?) ORDER BY access_type";
		else
			return "SELECT DISTINCT access_type FROM read_access WHERE resource_id = ? AND resource_version = ? AND (access_type = 'ALL' OR organization_id = ?) ORDER BY access_type";
	}

	private PGobject uuidToPgObject(UUID uuid)
	{
		if (uuid == null)
			return null;

		try
		{
			PGobject o = new PGobject();
			o.setType("UUID");
			o.setValue(uuid.toString());
			return o;
		}
		catch (DataFormatException | SQLException e)
		{
			throw new RuntimeException(e);
		}
	}
}
