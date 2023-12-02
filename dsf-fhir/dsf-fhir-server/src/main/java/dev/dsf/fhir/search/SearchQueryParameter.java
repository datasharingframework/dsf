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
