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
package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public class AbstractNameOrAliasParameter<R extends Resource> extends AbstractStringParameter<R>
{
	public static final String PARAMETER_NAME = "name";

	private final String resourceColumn;
	private final Predicate<R> hasName;
	private final Function<R, String> getName;
	private final Predicate<R> hasAlias;
	private final Function<R, List<StringType>> getAlias;

	public AbstractNameOrAliasParameter(Class<R> resourceType, String resourceColumn, Predicate<R> hasName,
			Function<R, String> getName, Predicate<R> hasAlias, Function<R, List<StringType>> getAlias)
	{
		super(resourceType, PARAMETER_NAME);

		this.resourceColumn = resourceColumn;
		this.hasName = hasName;
		this.getName = getName;
		this.hasAlias = hasAlias;
		this.getAlias = getAlias;
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case STARTS_WITH,
					CONTAINS ->
				"(lower(" + resourceColumn
						+ "->>'name') LIKE ? OR EXISTS (SELECT 1 FROM (SELECT jsonb_array_elements_text("
						+ resourceColumn + "->'alias') AS alias) AS aliases WHERE alias LIKE ?))";

			case EXACT -> "(" + resourceColumn + "->>'name' = ? OR " + resourceColumn + "->'alias' ?? ?)";
		};
	}

	@Override
	public int getSqlParameterCount()
	{
		return 2;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException
	{
		switch (valueAndType.type)
		{
			case STARTS_WITH:
				statement.setString(parameterIndex, valueAndType.value.toLowerCase() + "%");
				return;

			case CONTAINS:
				statement.setString(parameterIndex, "%" + valueAndType.value.toLowerCase() + "%");
				return;

			case EXACT:
				statement.setString(parameterIndex, valueAndType.value);
				return;
		}
	}

	@Override
	protected boolean resourceMatches(R resource)
	{
		return (hasName.test(resource) && nameMatches(getName.apply(resource)))
				|| (hasAlias.test(resource) && aliasMatches(resource));
	}

	private boolean aliasMatches(R resource)
	{
		return getAlias.apply(resource).stream().filter(StringType::hasValue).map(StringType::getValue)
				.anyMatch(this::nameMatches);
	}

	private boolean nameMatches(String name)
	{
		return switch (valueAndType.type)
		{
			case STARTS_WITH -> name.toLowerCase().startsWith(valueAndType.value.toLowerCase());
			case CONTAINS -> name.toLowerCase().contains(valueAndType.value.toLowerCase());
			case EXACT -> Objects.equals(name, valueAndType.value);
		};
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(SELECT array_agg(name) FROM (SELECT " + resourceColumn
				+ "->>'name' AS name UNION SELECT jsonb_array_elements_text(" + resourceColumn
				+ "->'alias') AS name) AS names)" + sortDirectionWithSpacePrefix;
	}
}
