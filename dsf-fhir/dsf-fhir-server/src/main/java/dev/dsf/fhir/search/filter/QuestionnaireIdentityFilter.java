package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;

public class QuestionnaireIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_questionnaires";
	private static final String RESOURCE_ID_COLUMN = "questionnaire_id";

	public QuestionnaireIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public QuestionnaireIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
