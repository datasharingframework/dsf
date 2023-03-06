package dev.dsf.fhir.history.filter;

import org.hl7.fhir.r4.model.Endpoint;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.search.filter.EndpointIdentityFilter;

public class EndpointHistoryIdentityFilter extends EndpointIdentityFilter implements HistoryIdentityFilter
{
	private static final String RESOURCE_TYPE = Endpoint.class.getAnnotation(ResourceDef.class).name();

	public EndpointHistoryIdentityFilter(Identity identity)
	{
		super(identity, HistoryIdentityFilter.RESOURCE_TABLE, HistoryIdentityFilter.RESOURCE_ID_COLUMN);
	}

	@Override
	public String getFilterQuery()
	{
		return HistoryIdentityFilter.getFilterQuery(RESOURCE_TYPE, super.getFilterQuery());
	}
}
