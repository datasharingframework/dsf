package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.Questionnaire;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.search.filter.QuestionnaireIdentityFilter;

public class QuestionnaireHistoryIdentityFilter extends QuestionnaireIdentityFilter implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = Questionnaire.class.getAnnotation(ResourceDef.class).name();

	public QuestionnaireHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
