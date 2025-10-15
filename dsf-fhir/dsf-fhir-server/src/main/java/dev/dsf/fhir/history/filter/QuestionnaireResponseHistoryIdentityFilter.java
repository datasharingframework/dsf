package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;
import dev.dsf.fhir.search.filter.QuestionnaireResponseIdentityFilter;

public class QuestionnaireResponseHistoryIdentityFilter extends QuestionnaireResponseIdentityFilter
		implements HistoryIdentityFilter
{
	private static final FhirServerRole HISTORY_ROLE = FhirServerRoleImpl.history(ResourceType.QuestionnaireResponse);
	private static final String RESOURCE_TYPE = ResourceType.QuestionnaireResponse.name();

	public QuestionnaireResponseHistoryIdentityFilter(Identity identity)
	{
		super(identity, HISTORY_ROLE);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
