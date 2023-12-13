package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/MeasureReport-identifier", type = SearchParamType.TOKEN, documentation = "External identifier of the measure report to be returned")
public class MeasureReportIdentifier extends AbstractIdentifierParameter<MeasureReport>
{
	private static final String RESOURCE_COLUMN = "measure_report";

	public MeasureReportIdentifier()
	{
		super(RESOURCE_COLUMN);
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!(resource instanceof MeasureReport))
			return false;

		MeasureReport m = (MeasureReport) resource;

		return identifierMatches(m.getIdentifier());
	}
}
