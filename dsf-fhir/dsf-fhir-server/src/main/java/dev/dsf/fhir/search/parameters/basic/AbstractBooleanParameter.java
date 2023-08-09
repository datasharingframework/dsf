package dev.dsf.fhir.search.parameters.basic;

import java.util.List;

import org.hl7.fhir.r4.model.DomainResource;

import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;

public abstract class AbstractBooleanParameter<R extends DomainResource> extends AbstractSearchParameter<R>
{
	protected Boolean value;

	public AbstractBooleanParameter(String parameterName)
	{
		super(parameterName);
	}

	@Override
	public void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		if (queryParameterValue != null && !queryParameterValue.isEmpty())
		{
			switch (queryParameterValue)
			{
				case "true":
					value = true;
					break;
				case "false":
					value = false;
					break;
				default:
					value = null;
					errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							parameterName, queryParameterValue, "true or false expected"));
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
	public String getBundleUriQueryParameterName()
	{
		return parameterName;
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return String.valueOf(value);
	}
}
