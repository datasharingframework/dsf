package dev.dsf.fhir.search.parameters.user;

import dev.dsf.fhir.authentication.User;

public class QuestionnaireUserFilter extends AbstractMetaTagAuthorizationRoleUserFilter
{
	private static final String RESOURCE_TABLE = "current_questionnaires";
	private static final String RESOURCE_ID_COLUMN = "questionnaire_id";

	public QuestionnaireUserFilter(User user)
	{
		super(user, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public QuestionnaireUserFilter(User user, String resourceTable, String resourceIdColumn)
	{
		super(user, resourceTable, resourceIdColumn);
	}
}
