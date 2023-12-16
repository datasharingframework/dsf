package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractVersionParameter;

@SearchParameterDefinition(name = AbstractVersionParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Measure-version", type = SearchParamType.TOKEN, documentation = "The business version of the measure")
public class MeasureVersion extends AbstractVersionParameter<Measure>
{
	private static final String RESOURCE_COLUMN = "measure";

	public MeasureVersion()
	{
		this(RESOURCE_COLUMN);
	}

	public MeasureVersion(String resourceColumn)
	{
		super(resourceColumn);
	}

	@Override
	protected boolean instanceOf(Resource resource)
	{
		return resource instanceof Measure;
	}
}
