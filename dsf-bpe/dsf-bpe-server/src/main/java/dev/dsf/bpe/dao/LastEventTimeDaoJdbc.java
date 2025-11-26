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
package dev.dsf.bpe.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;

public class LastEventTimeDaoJdbc extends AbstractDaoJdbc implements LastEventTimeDao, InitializingBean
{
	private final String type;

	public LastEventTimeDaoJdbc(DataSource dataSource, String type)
	{
		super(dataSource);

		this.type = type;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(type, "type");
		if (type.isBlank())
			throw new IllegalArgumentException("type is blank");
	}

	@Override
	public Optional<LocalDateTime> readLastEventTime() throws SQLException
	{
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(
						"SELECT last_event FROM last_events WHERE type = ? AND last_event IS NOT NULL"))
		{
			statement.setString(1, type);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
					return Optional.of(result.getTimestamp(1).toLocalDateTime());
				else
					return Optional.empty();
			}
		}
	}

	@Override
	public LocalDateTime writeLastEventTime(LocalDateTime lastEvent) throws SQLException
	{
		Objects.requireNonNull(lastEvent, "lastEvent");

		lastEvent = lastEvent.truncatedTo(ChronoUnit.MILLIS);

		try (Connection connection = dataSource.getConnection())
		{
			connection.setReadOnly(false);

			try (PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO last_events VALUES (?, ?) ON CONFLICT (type) WHERE type = ? DO UPDATE SET last_event = ?"))
			{
				statement.setString(1, type);
				statement.setTimestamp(2, Timestamp.valueOf(lastEvent));
				statement.setString(3, type);
				statement.setTimestamp(4, Timestamp.valueOf(lastEvent));

				statement.execute();
			}
		}

		return lastEvent;
	}
}
