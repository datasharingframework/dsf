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
package dev.dsf.common.status.health;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

import javax.sql.DataSource;

public class DatabaseHealthCheck implements HealthCheck
{
	private final DataSource dataSource;

	/**
	 * @param dataSource
	 *            not <code>null</code>
	 */
	public DatabaseHealthCheck(DataSource dataSource)
	{
		this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
	}

	@Override
	public HealthStatus check()
	{
		try (Connection _ = dataSource.getConnection())
		{
			return new HealthStatus("Database", true, "Connection is healthy");
		}
		catch (SQLException e)
		{
			return new HealthStatus("Database", false, "Connection failed - " + e.getSQLState());
		}
	}
}
