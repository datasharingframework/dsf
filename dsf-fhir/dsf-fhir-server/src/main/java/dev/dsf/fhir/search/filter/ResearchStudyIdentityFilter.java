package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class ResearchStudyIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_research_studies";
	private static final String RESOURCE_ID_COLUMN = "research_study_id";

	public ResearchStudyIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public ResearchStudyIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
