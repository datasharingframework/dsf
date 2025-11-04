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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import jakarta.ws.rs.core.UriBuilder;

public class SearchQuery<R extends Resource> implements DbSearchQuery, Matcher
{
	public static final String PARAMETER_INCLUDE = "_include";
	public static final String PARAMETER_REVINCLUDE = "_revinclude";

	public static final String PARAMETER_SORT = "_sort";
	public static final String PARAMETER_PAGE = "_page";
	public static final String PARAMETER_COUNT = "_count";
	public static final String PARAMETER_FORMAT = "_format";
	public static final String PARAMETER_PRETTY = "_pretty";
	public static final String PARAMETER_SUMMARY = "_summary";

	public static final String[] STANDARD_PARAMETERS = { PARAMETER_SORT, PARAMETER_INCLUDE, PARAMETER_REVINCLUDE,
			PARAMETER_PAGE, PARAMETER_COUNT, PARAMETER_FORMAT, PARAMETER_PRETTY, PARAMETER_SUMMARY };

	private static final String[] SINGLE_VALUE_PARAMETERS = { PARAMETER_SORT, PARAMETER_PAGE, PARAMETER_COUNT,
			PARAMETER_FORMAT, PARAMETER_PRETTY, PARAMETER_SUMMARY };

	public static class SearchQueryBuilder<R extends Resource>
	{
		public static <R extends Resource> SearchQueryBuilder<R> create(Class<R> resourceType, String resourceTable,
				String resourceColumn, PageAndCount pageAndCount)
		{
			return new SearchQueryBuilder<>(resourceType, resourceTable, resourceColumn, pageAndCount);
		}

		private final Class<R> resourceType;
		private final String resourceTable;
		private final String resourceColumn;

		private final PageAndCount pageAndCount;

		private final List<SearchQueryParameterFactory<R>> searchParameters = new ArrayList<>();
		private final List<SearchQueryRevIncludeParameterFactory> revIncludeParameters = new ArrayList<>();

		private SearchQueryIdentityFilter identityFilter; // may be null

		private SearchQueryBuilder(Class<R> resourceType, String resourceTable, String resourceColumn,
				PageAndCount pageAndCount)
		{
			this.resourceType = resourceType;
			this.resourceTable = resourceTable;
			this.resourceColumn = resourceColumn;

			this.pageAndCount = pageAndCount;
		}

		public SearchQueryBuilder<R> with(SearchQueryIdentityFilter identityFilter)
		{
			this.identityFilter = identityFilter;
			return this;
		}

		public SearchQueryBuilder<R> with(SearchQueryParameterFactory<R> searchParameters)
		{
			this.searchParameters.add(searchParameters);
			return this;
		}

		public SearchQueryBuilder<R> with(List<SearchQueryParameterFactory<R>> searchParameters)
		{
			this.searchParameters.addAll(searchParameters);
			return this;
		}

		public SearchQueryBuilder<R> withRevInclude(SearchQueryRevIncludeParameterFactory revIncludeParameter)
		{
			this.revIncludeParameters.add(revIncludeParameter);
			return this;
		}

		public SearchQueryBuilder<R> withRevInclude(SearchQueryRevIncludeParameterFactory... revIncludeParameters)
		{
			return withRevInclude(List.of(revIncludeParameters));
		}

		public SearchQueryBuilder<R> withRevInclude(List<SearchQueryRevIncludeParameterFactory> revIncludeParameters)
		{
			this.revIncludeParameters.addAll(revIncludeParameters);
			return this;
		}

		public SearchQuery<R> build()
		{
			return new SearchQuery<>(resourceType, resourceTable, resourceColumn, identityFilter, pageAndCount,
					searchParameters, revIncludeParameters);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(SearchQuery.class);

	private final Class<R> resourceType;
	private final String resourceColumn;
	private final String resourceTable;

	private final SearchQueryIdentityFilter identityFilter;

	private final PageAndCount pageAndCount;

	private final Map<String, SearchQueryParameterFactory<R>> searchParameterFactoriesByParameterName = new HashMap<>();
	private final Map<String, SearchQueryParameterFactory<R>> searchParameterFactoriesBySortParameterName = new HashMap<>();
	private final Map<String, SearchQueryParameterFactory<R>> includeParameterFactoriesByValue = new HashMap<>();
	private final Map<String, SearchQueryRevIncludeParameterFactory> revIncludeParameterFactoriesByValue = new HashMap<>();

	private final List<SearchQueryParameter<R>> searchParameters = new ArrayList<>();
	private final List<SearchQuerySortParameterConfiguration> sortParameters = new ArrayList<>();
	private final List<SearchQueryIncludeParameterConfiguration> includeParameters = new ArrayList<>();
	private final List<SearchQueryIncludeParameterConfiguration> revIncludeParameters = new ArrayList<>();
	private final List<SearchQueryParameterError> errors = new ArrayList<>();

	private String filterQuery;
	private String sortSql;
	private String includeSql;
	private String revIncludeSql;

	SearchQuery(Class<R> resourceType, String resourceTable, String resourceColumn,
			SearchQueryIdentityFilter identityFilter, PageAndCount pageAndCount,
			List<SearchQueryParameterFactory<R>> searchParameterFactories,
			List<SearchQueryRevIncludeParameterFactory> searchRevIncludeParameterFactories)
	{
		this.resourceType = resourceType;
		this.resourceTable = resourceTable;
		this.resourceColumn = resourceColumn;

		this.identityFilter = identityFilter;

		this.pageAndCount = pageAndCount;

		if (searchParameterFactories != null)
		{
			searchParameterFactories.forEach(f ->
			{
				f.getNameAndModifiedNames().forEach(name ->
				{
					SearchQueryParameterFactory<R> existingMapping = searchParameterFactoriesByParameterName
							.putIfAbsent(name, f);

					if (existingMapping != null)
						throw new RuntimeException("More than one " + SearchQueryParameter.class.getName()
								+ " configured for parameter name " + name);
				});

				f.getSortNames().forEach(name ->
				{
					SearchQueryParameterFactory<R> existingMapping = searchParameterFactoriesBySortParameterName
							.putIfAbsent(name, f);

					if (existingMapping != null)
						throw new RuntimeException("More than one " + SearchQueryParameter.class.getName()
								+ " configured for sort parameter name " + name);
				});

				if (f.isIncludeParameter())
				{
					f.getIncludeParameterValues().forEach(value ->
					{
						SearchQueryParameterFactory<R> existingMapping = includeParameterFactoriesByValue
								.putIfAbsent(value, f);

						if (existingMapping != null)
							throw new RuntimeException("More than one " + SearchQueryParameter.class.getName()
									+ " configured for include parameter value " + value);
					});
				}
			});
		}

		if (searchRevIncludeParameterFactories != null)
		{
			searchRevIncludeParameterFactories.forEach(f -> f.getRevIncludeParameterValues().forEach(value ->
			{
				SearchQueryRevIncludeParameterFactory existingMapping = revIncludeParameterFactoriesByValue
						.putIfAbsent(value, f);

				if (existingMapping != null)
					throw new RuntimeException("More than one " + SearchQueryRevIncludeParameter.class.getName()
							+ " configured for revinclude parameter value " + value);
			}));
		}
	}

	public SearchQuery<R> configureParameters(Map<String, List<String>> queryParameters)
	{
		checkSingleValueParameters(queryParameters);

		filterQuery = createFilterQuery(queryParameters);

		includeSql = createIncludeSql(queryParameters.getOrDefault(PARAMETER_INCLUDE, List.of()));
		revIncludeSql = createRevIncludeSql(queryParameters.getOrDefault(PARAMETER_REVINCLUDE, List.of()));

		sortSql = createSortSql(queryParameters.getOrDefault(PARAMETER_SORT, List.of()));

		return this;
	}

	private void checkSingleValueParameters(Map<String, List<String>> queryParameters)
	{
		Arrays.stream(SINGLE_VALUE_PARAMETERS).forEach(parameter ->
		{
			List<String> values = queryParameters.get(parameter);
			if (values != null && values.size() > 1)
			{
				errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNSUPPORTED_NUMBER_OF_VALUES,
						parameter, null, "More than one query parameter `" + parameter + "`"));
			}
		});
	}

	private String createFilterQuery(Map<String, List<String>> queryParameters)
	{
		queryParameters.entrySet().stream()
				.filter(e -> Arrays.stream(STANDARD_PARAMETERS).noneMatch(p -> p.equals(e.getKey()))).forEach(e ->
				{
					SearchQueryParameterFactory<R> queryParameterFactory = searchParameterFactoriesByParameterName
							.get(e.getKey());
					if (queryParameterFactory != null)
					{
						e.getValue().stream().filter(v -> v != null && !v.isBlank())
								.forEach(value -> searchParameters.add(queryParameterFactory.createQueryParameter()
										.configure(errors, e.getKey(), value)));
					}
					else
					{
						errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNSUPPORTED_PARAMETER,
								e.getKey(), null, "Query parameter `" + e.getKey() + "` not supported"));
					}
				});

		Stream<String> elements = searchParameters.stream().filter(SearchQueryParameter::isDefined)
				.map(SearchQueryParameter::getFilterQuery);

		if (identityFilter != null && !identityFilter.getFilterQuery().isEmpty())
			elements = Stream.concat(Stream.of(identityFilter.getFilterQuery()), elements);

		return elements.collect(Collectors.joining(" AND "));
	}

	public List<SearchQueryParameterError> getUnsupportedQueryParameters()
	{
		return errors;
	}

	private String createSortSql(List<String> sortParameterValues)
	{
		if (sortParameterValues.size() <= 0)
			return "";

		final String sortParameterValue = sortParameterValues.get(0);

		if (sortParameterValue == null || sortParameterValue.isBlank())
			return "";

		Set<String> supportedSortValues = new HashSet<>();
		for (String value : sortParameterValue.split(","))
		{
			if (value != null && !value.isBlank())
			{
				SearchQueryParameterFactory<R> sortParameterFactory = searchParameterFactoriesBySortParameterName
						.get(value);
				if (sortParameterFactory != null)
				{
					if (!supportedSortValues.contains(sortParameterFactory.getName()))
					{
						supportedSortValues.add(sortParameterFactory.getName());
						sortParameters
								.add(sortParameterFactory.createQuerySortParameter().configureSort(errors, value));
					}
					else
					{
						errors.add(new SearchQueryParameterError(
								SearchQueryParameterErrorType.UNSUPPORTED_NUMBER_OF_VALUES, PARAMETER_SORT, null,
								"More than one " + PARAMETER_SORT + " query parameter valus `" + value + "`"));
					}
				}
				else
				{
					errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							PARAMETER_SORT, null,
							PARAMETER_SORT + " query parameter value `" + value + "` not supported"));
				}
			}
		}

		return sortParameters.isEmpty() ? ""
				: sortParameters.stream().map(SearchQuerySortParameterConfiguration::getSql)
						.collect(Collectors.joining(", ", " ORDER BY ", ""));
	}

	private String createIncludeSql(List<String> includeParameterValues)
	{
		Set<String> supportedIncludeValues = new HashSet<>();
		for (String value : includeParameterValues)
		{
			if (value != null && !value.isBlank())
			{
				SearchQueryParameterFactory<R> includeParameterFactory = includeParameterFactoriesByValue.get(value);
				if (includeParameterFactory != null)
				{
					if (!supportedIncludeValues.contains(value))
					{
						supportedIncludeValues.add(value);
						includeParameters.add(
								includeParameterFactory.createQueryIncludeParameter().configureInclude(errors, value));
					}
					else
					{
						errors.add(new SearchQueryParameterError(
								SearchQueryParameterErrorType.UNSUPPORTED_NUMBER_OF_VALUES, PARAMETER_INCLUDE, null,
								"More than one " + PARAMETER_INCLUDE + " query parameter value " + value));
					}
				}
				else
				{
					errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							PARAMETER_INCLUDE, null,
							PARAMETER_INCLUDE + " query parameter value " + value + " not supported"));
				}
			}
		}

		return includeParameters.isEmpty() ? ""
				: includeParameters.stream().map(SearchQueryIncludeParameterConfiguration::getSql)
						.collect(Collectors.joining(", ", ", ", ""));
	}

	private String createRevIncludeSql(List<String> revIncludeParameterValues)
	{
		Set<String> supportedRevIncludeValues = new HashSet<>();
		for (String value : revIncludeParameterValues)
		{
			if (value != null && !value.isBlank())
			{
				SearchQueryRevIncludeParameterFactory revIncludeParameterFactory = revIncludeParameterFactoriesByValue
						.get(value);
				if (revIncludeParameterFactory != null)
				{
					if (!supportedRevIncludeValues.contains(value))
					{
						supportedRevIncludeValues.add(value);
						revIncludeParameters.add(revIncludeParameterFactory.createQueryRevIncludeParameter()
								.configureRevInclude(errors, value));
					}
					else
					{
						errors.add(new SearchQueryParameterError(
								SearchQueryParameterErrorType.UNSUPPORTED_NUMBER_OF_VALUES, PARAMETER_REVINCLUDE, null,
								"More than one " + PARAMETER_REVINCLUDE + " query parameter value " + value));
					}
				}
				else
				{
					errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							PARAMETER_REVINCLUDE, null,
							PARAMETER_REVINCLUDE + " query parameter value " + value + " not supported"));
				}
			}
		}

		return revIncludeParameters.isEmpty() ? ""
				: revIncludeParameters.stream().map(SearchQueryIncludeParameterConfiguration::getSql)
						.collect(Collectors.joining(", ", ", ", ""));
	}

	@Override
	public String getCountSql()
	{
		String countQueryMain = "SELECT count(*) FROM current_" + resourceTable;

		return countQueryMain + (!filterQuery.isEmpty() ? " WHERE " + filterQuery : "");
	}

	@Override
	public String getSearchSql()
	{
		String searchQueryMain = "SELECT " + resourceColumn + includeSql + revIncludeSql + " FROM current_"
				+ resourceTable;

		return searchQueryMain + (!filterQuery.isEmpty() ? " WHERE " + filterQuery : "") + sortSql
				+ pageAndCount.getSql();
	}

	@Override
	public void modifyStatement(PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException
	{
		try
		{
			List<SearchQueryParameter<?>> filtered = searchParameters.stream().filter(SearchQueryParameter::isDefined)
					.collect(Collectors.toList());

			int index = 0;
			if (identityFilter != null)
			{
				while (index < identityFilter.getSqlParameterCount())
				{
					int i = ++index;
					identityFilter.modifyStatement(i, i, statement);
				}
			}

			for (SearchQueryParameter<?> q : filtered)
				for (int i = 0; i < q.getSqlParameterCount(); i++)
					q.modifyStatement(++index, i + 1, statement, arrayCreator);
		}
		catch (SQLException e)
		{
			logger.debug("Error while modifying prepared statement '{}'", statement.toString(), e);
			throw e;
		}
	}

	@Override
	public PageAndCount getPageAndCount()
	{
		return pageAndCount;
	}

	public UriBuilder configureBundleUri(UriBuilder bundleUri)
	{
		Objects.requireNonNull(bundleUri, "bundleUri");

		searchParameters.stream().filter(SearchQueryParameter::isDefined)
				.collect(Collectors.toMap(SearchQueryParameter::getBundleUriQueryParameterName,
						p -> List.of(p.getBundleUriQueryParameterValue()), (v1, v2) ->
						{
							List<String> list = new ArrayList<>(v1);
							list.addAll(v2);
							return list;
						}))
				.entrySet().stream().sorted(Comparator.comparing(Entry::getKey))
				.forEach(e -> bundleUri.replaceQueryParam(e.getKey(), e.getValue().toArray()));

		if (!sortParameters.isEmpty())
		{
			String values = sortParameters.stream()
					.map(SearchQuerySortParameterConfiguration::getBundleUriQueryParameterValuePart)
					.collect(Collectors.joining(","));
			bundleUri.replaceQueryParam(PARAMETER_SORT, values);
		}
		if (!includeParameters.isEmpty())
		{
			Object[] values = includeParameters.stream()
					.map(SearchQueryIncludeParameterConfiguration::getBundleUriQueryParameterValues).toArray();
			bundleUri.replaceQueryParam(PARAMETER_INCLUDE, values);
		}
		if (!revIncludeParameters.isEmpty())
		{
			Object[] values = revIncludeParameters.stream()
					.map(SearchQueryIncludeParameterConfiguration::getBundleUriQueryParameterValues).toArray();
			bundleUri.replaceQueryParam(PARAMETER_REVINCLUDE, values);
		}

		return bundleUri;
	}

	@Override
	public Class<R> getResourceType()
	{
		return resourceType;
	}

	@Override
	public void resloveReferencesForMatching(Resource resource, DaoProvider daoProvider) throws SQLException
	{
		if (resource == null || !getResourceType().isInstance(resource))
			return;

		List<SQLException> exceptions = searchParameters.stream().filter(SearchQueryParameter::isDefined).map(p ->
		{
			try
			{
				p.resolveReferencesForMatching(resource, daoProvider);
				return null;
			}
			catch (SQLException e)
			{
				return e;
			}
		}).filter(e -> e != null).collect(Collectors.toList());

		if (!exceptions.isEmpty())
		{
			SQLException sqlException = new SQLException("Error while resoling references");
			exceptions.forEach(sqlException::addSuppressed);
			throw sqlException;
		}
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (resource == null || !getResourceType().isInstance(resource))
			return false;

		// returns true if no search parameters configured
		return searchParameters.stream().filter(SearchQueryParameter::isDefined).allMatch(p -> p.matches(resource));
	}

	@Override
	public void modifyIncludeResource(Resource resource, int columnIndex, Connection connection) throws SQLException
	{
		int includeParameterCount = includeParameters.size();
		int revIncludeParameterCount = revIncludeParameters.size();

		if (includeParameterCount > 0 && columnIndex - 1 <= includeParameterCount)
		{
			includeParameters.get(columnIndex - 2).modifyIncludeResource(resource, connection);
		}
		else if (revIncludeParameters.size() > 0 && columnIndex - 1 - includeParameterCount <= revIncludeParameterCount)
		{
			revIncludeParameters.get(columnIndex - 2 - includeParameterCount).modifyIncludeResource(resource,
					connection);
		}
		else
		{
			logger.warn(
					"Unexpected column-index {}, column-index - 1 larger than include ({}) + revinclude ({}) parameter count {}",
					columnIndex, includeParameterCount, revIncludeParameterCount,
					includeParameterCount + revIncludeParameterCount);
			throw new IllegalStateException(
					"Unexpected column-index " + columnIndex + ", column-index - 1 larger than include ("
							+ includeParameterCount + ") + revinclude (" + revIncludeParameterCount
							+ ") parameter count " + (includeParameterCount + revIncludeParameterCount));
		}
	}
}
