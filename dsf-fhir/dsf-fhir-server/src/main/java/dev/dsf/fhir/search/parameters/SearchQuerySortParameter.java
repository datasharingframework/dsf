package dev.dsf.fhir.search.parameters;

import java.util.List;

import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQuerySortParameterConfiguration;

public interface SearchQuerySortParameter
{
	/**
	 * @param errors
	 *            not <code>null</code>
	 * @param queryParameterSortValue
	 *            one of (parameterName, +parameterName or -parameterName), not <code>null</code> and not blank
	 * @return
	 */
	SearchQuerySortParameterConfiguration configureSort(List<? super SearchQueryParameterError> errors,
			String queryParameterSortValue);
}
