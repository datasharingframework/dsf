package dev.dsf.fhir.history.user;

import org.hl7.fhir.r4.model.MeasureReport;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.search.parameters.user.MeasureReportUserFilter;

public class MeasureReportHistoryUserFilter extends MeasureReportUserFilter implements HistoryUserFilter
{
	private static final String RESOURCE_TYPE = MeasureReport.class.getAnnotation(ResourceDef.class).name();

	public MeasureReportHistoryUserFilter(User user)
	{
		super(user, HistoryUserFilter.RESOURCE_TABLE, HistoryUserFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryUserFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
