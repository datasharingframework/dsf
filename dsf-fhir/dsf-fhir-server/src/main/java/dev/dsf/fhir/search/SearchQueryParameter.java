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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.parameters.SearchQuerySortParameter;

public interface SearchQueryParameter<R extends Resource> extends MatcherParameter, SearchQuerySortParameter
{
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface SearchParameterDefinition
	{
		String name();

		String definition();

		SearchParamType type();

		String documentation();
	}

	/**
	 * @param errors
	 *            not <code>null</code>
	 * @param queryParameterName
	 *            not <code>null</code> and not blank
	 * @param queryParameterValue
	 *            not <code>null</code> and not blank
	 * @return the current instance
	 */
	SearchQueryParameter<R> configure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue);

	boolean isDefined();

	String getFilterQuery();

	int getSqlParameterCount();

	void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException;

	/**
	 * Only called if {@link #isDefined()} returns <code>true</code>
	 *
	 * @return not <code>null</code>, not blank
	 */
	String getBundleUriQueryParameterName();

	/**
	 * Only called if {@link #isDefined()} returns <code>true</code>
	 *
	 * @return not <code>null</code>, not blank
	 */
	String getBundleUriQueryParameterValue();

	String getParameterName();
}
