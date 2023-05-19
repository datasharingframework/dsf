package dev.dsf.fhir.search.parameters.basic;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.DomainResource;

import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import dev.dsf.fhir.search.parameters.basic.AbstractCanonicalUrlParameter.UriSearchType;
import jakarta.ws.rs.core.UriBuilder;

public abstract class AbstractBooleanParameter<R extends DomainResource> extends AbstractSearchParameter<R>
{
	protected Boolean value;

	public AbstractBooleanParameter(String parameterName)
	{
		super(parameterName);
	}

	@Override
	protected void configureSearchParameter(Map<String, List<String>> queryParameters)
	{
		List<String> values = queryParameters.getOrDefault(parameterName + UriSearchType.PRECISE.modifier,
				Collections.emptyList());
		if (values.size() > 1)
			addError(new SearchQueryParameterError(SearchQueryParameterErrorType.UNSUPPORTED_NUMBER_OF_VALUES,
					parameterName, values));

		String param = getFirst(queryParameters, parameterName);
		if (param != null && !param.isEmpty())
		{
			switch (param)
			{
				case "true":
					value = true;
					break;
				case "false":
					value = false;
					break;
				default:
					value = null;
					addError(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							parameterName, values, "true or false expected"));
					break;
			}
		}
	}

	@Override
	public boolean isDefined()
	{
		return value != null;
	}

	@Override
	public void modifyBundleUri(UriBuilder bundleUri)
	{
		if (isDefined())
			bundleUri.replaceQueryParam(parameterName, String.valueOf(value));
	}
}
