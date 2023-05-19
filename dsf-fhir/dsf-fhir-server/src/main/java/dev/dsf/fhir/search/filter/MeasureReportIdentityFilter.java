package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;

public class MeasureReportIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_measure_reports";
	private static final String RESOURCE_ID_COLUMN = "measure_report_id";

	public MeasureReportIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public MeasureReportIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
