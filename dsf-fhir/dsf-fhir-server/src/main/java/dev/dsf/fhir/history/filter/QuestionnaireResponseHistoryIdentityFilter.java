package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.search.filter.QuestionnaireResponseIdentityFilter;

public class QuestionnaireResponseHistoryIdentityFilter extends QuestionnaireResponseIdentityFilter
		implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = QuestionnaireResponse.class.getAnnotation(ResourceDef.class).name();

	public QuestionnaireResponseHistoryIdentityFilter(Identity identity)
	{
		super(identity);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
