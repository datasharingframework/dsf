package dev.dsf.fhir.search;

import java.util.List;

public interface SearchQueryRevIncludeParameter
{
	/**
	 * @param errors
	 *            not <code>null</code>
	 * @param queryParameterRevIncludeValue
	 *            not <code>null</code>, not blank
	 * @return
	 */
	SearchQueryIncludeParameterConfiguration configureRevInclude(List<? super SearchQueryParameterError> errors,
			String queryParameterRevIncludeValue);
}
