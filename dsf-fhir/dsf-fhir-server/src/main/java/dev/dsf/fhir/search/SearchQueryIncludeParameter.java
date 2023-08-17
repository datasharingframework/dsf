package dev.dsf.fhir.search;

import java.util.List;

import org.hl7.fhir.r4.model.Resource;

public interface SearchQueryIncludeParameter<R extends Resource>
{
	/**
	 * @param errors
	 *            not <code>null</code>
	 * @param queryParameterIncludeValue
	 *            not <code>null</code>, not blank
	 * @return
	 */
	SearchQueryIncludeParameterConfiguration configureInclude(List<? super SearchQueryParameterError> errors,
			String queryParameterIncludeValue);
}
