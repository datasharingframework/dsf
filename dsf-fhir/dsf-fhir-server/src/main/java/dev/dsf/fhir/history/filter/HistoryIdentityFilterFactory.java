package dev.dsf.fhir.history.filter;

import java.util.List;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.conf.Identity;

public interface HistoryIdentityFilterFactory
{
	HistoryIdentityFilter getIdentityFilter(Identity identity, Class<? extends Resource> resourceType);

	List<HistoryIdentityFilter> getIdentityFilters(Identity identity);
}
