package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.ResearchStudy;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.search.filter.ResearchStudyIdentityFilter;

public class ResearchStudyHistoryIdentityFilter extends ResearchStudyIdentityFilter implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = ResearchStudy.class.getAnnotation(ResourceDef.class).name();

	public ResearchStudyHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
