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

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

public interface LastEventTimeDao
{
	Optional<LocalDateTime> readLastEventTime() throws SQLException;

	/**
	 * @param lastEvent
	 *            not <code>null</code>
	 * @return the given <b>lastEvent</b> with millisecond precision
	 * @throws SQLException
	 *             if a database access error occurs
	 * @see LocalDateTime#truncatedTo(java.time.temporal.TemporalUnit)
	 */
	LocalDateTime writeLastEventTime(LocalDateTime lastEvent) throws SQLException;

	default Date writeLastEventTime(Date lastEvent) throws SQLException
	{
		LocalDateTime ldt = writeLastEventTime(
				lastEvent == null ? null : LocalDateTime.ofInstant(lastEvent.toInstant(), ZoneId.systemDefault()));

		return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
	}
}
