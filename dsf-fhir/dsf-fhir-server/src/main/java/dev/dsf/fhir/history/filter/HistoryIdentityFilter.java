package dev.dsf.fhir.history.filter;

import dev.dsf.fhir.search.SearchQueryIdentityFilter;

public interface HistoryIdentityFilter extends SearchQueryIdentityFilter
{
	String RESOURCE_ID_COLUMN = "id";
	String RESOURCE_COLUMN = "resource";
	String RESOURCE_TABLE = "history";

	static String getFilterQuery(String resourceType, String filterQuery)
	{
		if (filterQuery == null || filterQuery.isBlank())
			return "(type = '" + resourceType + "')";
		else
			return "(type = '" + resourceType + "' AND " + filterQuery + ")";
	}

	default boolean isDefined()
	{
		String filterQuery = getFilterQuery();
		return filterQuery != null && !filterQuery.isBlank();
	}
}
