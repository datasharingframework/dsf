package dev.dsf.fhir.history.user;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.search.parameters.user.QuestionnaireResponseUserFilter;

public class QuestionnaireResponseHistoryUserFilter extends QuestionnaireResponseUserFilter implements HistoryUserFilter
{
	private static final String RESOURCE_TYPE = QuestionnaireResponse.class.getAnnotation(ResourceDef.class).name();

	public QuestionnaireResponseHistoryUserFilter(User user)
	{
		super(user);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryUserFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
