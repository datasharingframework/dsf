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
package dev.dsf.fhir.search;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public interface SearchQueryIdentityFilter
{
	/**
	 * @return not <code>null</code>, empty {@link String} if resources should not be filtered
	 */
	String getFilterQuery();

	/**
	 * @return {@code >=0}, 0 if {@link #getFilterQuery()} returns empty {@link String}
	 */
	int getSqlParameterCount();

	/**
	 * @param parameterIndex
	 *            {@code >= 1}
	 * @param subqueryParameterIndex
	 *            [1 ... {@link #getSqlParameterCount()}]
	 * @param statement
	 *            not <code>null</code>
	 * @throws SQLException
	 *             if errors occur during modification of the statement
	 */
	void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement)
			throws SQLException;
}
