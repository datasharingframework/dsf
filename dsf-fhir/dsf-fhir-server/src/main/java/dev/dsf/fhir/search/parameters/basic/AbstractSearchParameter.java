package dev.dsf.fhir.search.parameters.basic;

import java.util.List;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQuerySortParameterConfiguration;
import dev.dsf.fhir.search.SearchQuerySortParameterConfiguration.SortDirection;
import dev.dsf.fhir.search.parameters.SearchQuerySortParameter;

public abstract class AbstractSearchParameter<R extends Resource>
		implements SearchQueryParameter<R>, SearchQuerySortParameter
{
	protected final String parameterName;

	public AbstractSearchParameter(String parameterName)
	{
		this.parameterName = parameterName;
	}

	@Override
	public final String getParameterName()
	{
		return parameterName;
	}

	protected final IllegalStateException notDefined()
	{
		return new IllegalStateException("not defined");
	}

	@Override
	public SearchQueryParameter<R> configure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		doConfigure(errors, queryParameterName, queryParameterValue);
		return this;
	}

	protected abstract void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue);

	@Override
	public SearchQuerySortParameterConfiguration configureSort(List<? super SearchQueryParameterError> errors,
			String queryParameterSortValue)
	{
		SortDirection direction = SortDirection.fromString(queryParameterSortValue);
		return new SearchQuerySortParameterConfiguration(getSortSql(direction.getSqlModifierWithSpacePrefix()),
				parameterName, direction);
	}

	protected abstract String getSortSql(String sortDirectionWithSpacePrefix);
}
