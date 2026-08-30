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

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.dao.jdbc.PgObjectFactory;
import dev.dsf.fhir.function.BiFunctionWithSqlException;

public class SearchQueryParameterOrGroup<R extends Resource> implements SearchQueryParameter<R>
{
	private final Supplier<SearchQueryParameter<R>> supplier;

	private List<SearchQueryParameter<R>> searchParameters;
	private int[] sqlParameterCounts;

	public SearchQueryParameterOrGroup(Supplier<SearchQueryParameter<R>> supplier)
	{
		this.supplier = supplier;
	}

	@Override
	public boolean matches(Resource resource)
	{
		return searchParameters.stream().filter(SearchQueryParameter::isDefined).anyMatch(p -> p.matches(resource));
	}

	@Override
	public SearchQuerySortParameterConfiguration configureSort(List<? super SearchQueryParameterError> errors,
			String queryParameterSortValue)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public SearchQueryParameter<R> configure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		searchParameters = splitValuesForOr(queryParameterValue).stream().filter(Predicate.not(String::isBlank))
				.map(orValue -> supplier.get().configure(errors, queryParameterName, orValue)).toList();

		sqlParameterCounts = searchParameters.stream().mapToInt(SearchQueryParameter::getSqlParameterCount).toArray();

		return this;
	}

	/**
	 * Splits a single FHIR search parameter value into its comma separated OR-parts. Commas escaped as <code>\,</code>
	 * are treated as literal characters (and unescaped); other escape sequences are left untouched for the parameter
	 * type specific parsing.
	 *
	 * @param value
	 *            not <code>null</code>
	 * @return the OR-parts, never empty
	 */
	private List<String> splitValuesForOr(String value)
	{
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();

		for (int i = 0; i < value.length(); i++)
		{
			char c = value.charAt(i);
			if (c == '\\' && i + 1 < value.length() && value.charAt(i + 1) == ',')
			{
				current.append(',');
				i++;
			}
			else if (c == ',')
			{
				values.add(current.toString());
				current.setLength(0);
			}
			else
				current.append(c);
		}
		values.add(current.toString());

		return values;
	}

	@Override
	public boolean isDefined()
	{
		if (searchParameters == null)
			throw new IllegalStateException("not configured");

		return searchParameters.stream().anyMatch(SearchQueryParameter::isDefined);
	}

	@Override
	public String getFilterQuery()
	{
		if (searchParameters == null)
			throw new IllegalStateException("not configured");

		List<String> filters = searchParameters.stream().filter(SearchQueryParameter::isDefined)
				.map(SearchQueryParameter::getFilterQuery).collect(Collectors.toList());

		if (filters.isEmpty())
			return "";
		else if (filters.size() == 1)
			return filters.get(0);
		else
			return filters.stream().collect(Collectors.joining(" OR ", "(", ")"));
	}

	@Override
	public int getSqlParameterCount()
	{
		if (sqlParameterCounts == null)
			throw new IllegalStateException("not configured");

		return IntStream.of(sqlParameterCounts).sum();
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator, PgObjectFactory pgObjectFactory)
			throws SQLException
	{
		if (sqlParameterCounts == null)
			throw new IllegalStateException("not configured");

		int remaining = subqueryParameterIndex;
		for (int orElement = 0; orElement < sqlParameterCounts.length; orElement++)
		{
			if (remaining <= sqlParameterCounts[orElement])
			{
				int orSubqueryParameterIndex = remaining;

				searchParameters.get(orElement).modifyStatement(parameterIndex, orSubqueryParameterIndex, statement,
						arrayCreator, pgObjectFactory);
				return;
			}

			remaining -= sqlParameterCounts[orElement];
		}

		throw new IllegalStateException("Invalid subqueryParameterIndex: " + subqueryParameterIndex);
	}

	@Override
	public String getBundleUriQueryParameterName()
	{
		if (searchParameters == null)
			throw new IllegalStateException("not configured");

		return searchParameters.get(0).getBundleUriQueryParameterName();
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		if (searchParameters == null)
			throw new IllegalStateException("not configured");

		return searchParameters.stream().filter(SearchQueryParameter::isDefined)
				.map(SearchQueryParameter::getBundleUriQueryParameterValue).map(v -> v.replace(",", "\\,"))
				.collect(Collectors.joining(","));
	}

	public List<SearchQueryParameter<R>> getSearchParameters()
	{
		if (searchParameters == null)
			throw new IllegalStateException("not configured");

		return List.copyOf(searchParameters);
	}
}
