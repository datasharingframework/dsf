package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.Subscription;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.search.filter.SubscriptionIdentityFilter;

public class SubscriptionHistoryIdentityFilter extends SubscriptionIdentityFilter implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = Subscription.class.getAnnotation(ResourceDef.class).name();

	public SubscriptionHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
