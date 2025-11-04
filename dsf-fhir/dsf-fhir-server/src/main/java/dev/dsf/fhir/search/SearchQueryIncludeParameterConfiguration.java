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

import java.sql.Connection;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiConsumerWithSqlException;

public class SearchQueryIncludeParameterConfiguration
{
	private final String sql;
	private final IncludeParts includeParts;

	private final BiConsumerWithSqlException<Resource, Connection> includeResourceModifier;

	public SearchQueryIncludeParameterConfiguration(String sql, IncludeParts includeParts)
	{
		this(sql, includeParts, null);
	}

	/**
	 * @param sql
	 *            not <code>null</code>
	 * @param includeParts
	 *            not <code>null</code>
	 * @param includeResourceModifier
	 *            Use this {@link BiConsumerWithSqlException} to modify the include resources. This consumer can be used
	 *            if the resources returned by the include SQL are not complete and additional content needs to be
	 *            retrieved from a not included column. For example the content of a {@link Binary} resource might not
	 *            be stored in the json column.
	 */
	public SearchQueryIncludeParameterConfiguration(String sql, IncludeParts includeParts,
			BiConsumerWithSqlException<Resource, Connection> includeResourceModifier)
	{
		this.sql = sql;
		this.includeParts = includeParts;
		this.includeResourceModifier = includeResourceModifier;
	}

	public String getBundleUriQueryParameterValues()
	{
		return includeParts.toBundleUriQueryParameterValue();
	}

	public String getSql()
	{
		return sql;
	}

	public void modifyIncludeResource(Resource resource, Connection connection) throws SQLException
	{
		if (includeResourceModifier != null)
			includeResourceModifier.accept(resource, connection);
	}
}
