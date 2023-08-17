package dev.dsf.fhir.search.parameters.rev.include;

import java.sql.Connection;
import java.util.List;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.IncludeParts;
import dev.dsf.fhir.search.SearchQueryIncludeParameterConfiguration;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryRevIncludeParameter;

public abstract class AbstractRevIncludeParameter implements SearchQueryRevIncludeParameter
{
	@Override
	public SearchQueryIncludeParameterConfiguration configureRevInclude(List<? super SearchQueryParameterError> errors,
			String queryParameterRevIncludeValue)
	{
		IncludeParts includeParts = IncludeParts.fromString(queryParameterRevIncludeValue);
		String revIncludeSql = getRevIncludeSql(includeParts);

		if (revIncludeSql != null)
			return new SearchQueryIncludeParameterConfiguration(revIncludeSql, includeParts,
					(resource, connection) -> modifyRevIncludeResource(includeParts, resource, connection));
		else
			return null;

	}

	protected abstract String getRevIncludeSql(IncludeParts includeParts);

	/**
	 * Use this method to modify the revinclude resources. This method can be used if the resources returned by the
	 * include SQL are not complete and additional content needs to be retrieved from a not included column. For example
	 * the content of a {@link Binary} resource might not be stored in the json column.
	 *
	 * @param includeParts
	 *            not <code>null</code>
	 * @param resource
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 */
	protected abstract void modifyRevIncludeResource(IncludeParts includeParts, Resource resource,
			Connection connection);
}
