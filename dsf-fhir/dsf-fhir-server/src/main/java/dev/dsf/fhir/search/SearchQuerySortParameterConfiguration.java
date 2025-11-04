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

public class SearchQuerySortParameterConfiguration
{
	public enum SortDirection
	{
		ASC("", ""), DESC(" DESC", "-");

		private final String sqlModifier;
		private final String urlModifier;

		SortDirection(String sqlModifier, String urlModifier)
		{
			this.sqlModifier = sqlModifier;
			this.urlModifier = urlModifier;
		}

		public String getSqlModifierWithSpacePrefix()
		{
			return sqlModifier;
		}

		public String getUrlModifier()
		{
			return urlModifier;
		}

		public static SortDirection fromString(String sortParameter)
		{
			if ('-' == sortParameter.charAt(0))
				return DESC;
			else
				return ASC;
		}
	}

	private final String sql;
	private final String parameterName;
	private final SortDirection direction;

	public SearchQuerySortParameterConfiguration(String sql, String parameterName, SortDirection direction)
	{
		this.sql = sql;
		this.parameterName = parameterName;
		this.direction = direction;
	}

	public String getSql()
	{
		return sql;
	}

	public SortDirection getDirection()
	{
		return direction;
	}

	public String getParameterName()
	{
		return parameterName;
	}

	public String getBundleUriQueryParameterValuePart()
	{
		return getDirection().getUrlModifier() + getParameterName();
	}
}
