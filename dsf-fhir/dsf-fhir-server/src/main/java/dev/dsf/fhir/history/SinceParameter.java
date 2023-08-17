package dev.dsf.fhir.history;

import java.util.List;

import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

public class SinceParameter extends AbstractDateTimeParameter<DomainResource>
{
	public static final String PARAMETER_NAME = "_since";

	public SinceParameter()
	{
		super(PARAMETER_NAME, "last_updated");
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (!DateTimeSearchType.EQ.equals(valueAndType.searchType)
				|| !DateTimeType.ZONED_DATE_TIME.equals(valueAndType.type))
		{
			errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE, parameterName,
					queryParameterValue, "Not instant"));
			valueAndType = null;
		}
		else
		{
			valueAndType = new DateTimeValueAndTypeAndSearchType(valueAndType.value, valueAndType.type,
					DateTimeSearchType.GE);
		}
	}

	@Override
	public boolean matches(Resource resource)
	{
		// Not implemented for history
		throw new UnsupportedOperationException();
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		// Not implemented for history
		throw new UnsupportedOperationException();
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return toUrlValue(valueAndType);
	}
}
