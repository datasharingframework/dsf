package dev.dsf.fhir.history;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.parameters.basic.AbstractDateTimeParameter;

public class AtParameter extends AbstractDateTimeParameter<Resource>
{
	public static final String PARAMETER_NAME = "_at";

	public AtParameter()
	{
		super(Resource.class, PARAMETER_NAME, "last_updated", null);
	}

	@Override
	protected boolean resourceMatches(Resource resource)
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
}
