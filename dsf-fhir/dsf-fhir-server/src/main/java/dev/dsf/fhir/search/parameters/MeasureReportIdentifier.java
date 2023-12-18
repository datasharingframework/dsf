package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.MeasureReport;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/MeasureReport-identifier", type = SearchParamType.TOKEN, documentation = "External identifier of the measure report to be returned")
public class MeasureReportIdentifier extends AbstractIdentifierParameter<MeasureReport>
{
	public MeasureReportIdentifier()
	{
		super(MeasureReport.class, "measure_report",
				listMatcher(MeasureReport::hasIdentifier, MeasureReport::getIdentifier));
	}
}
