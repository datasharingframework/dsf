package dev.dsf.fhir.history.user;

import java.util.List;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.authentication.User;

public interface HistoryUserFilterFactory
{
	HistoryUserFilter getUserFilter(User user, Class<? extends Resource> resourceType);

	List<HistoryUserFilter> getUserFilters(User user);
}
