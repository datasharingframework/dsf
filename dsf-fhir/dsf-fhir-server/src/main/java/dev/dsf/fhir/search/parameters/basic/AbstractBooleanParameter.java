package dev.dsf.fhir.search.parameters.basic;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Resource;

import com.google.common.base.Objects;

import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;

public abstract class AbstractBooleanParameter<R extends Resource> extends AbstractSearchParameter<R>
{
	private final Predicate<R> hasBoolean;
	private final Function<R, Boolean> getBoolean;

	protected Boolean value;

	public AbstractBooleanParameter(Class<R> resourceType, String parameterName, Predicate<R> hasBoolean,
			Function<R, Boolean> getBoolean)
	{
		super(resourceType, parameterName);

		this.hasBoolean = hasBoolean;
		this.getBoolean = getBoolean;
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

	@Override
	protected boolean resourceMatches(R resource)
	{
		return hasBoolean.test(resource) && Objects.equal(getBoolean.apply(resource), value);
	}
}
