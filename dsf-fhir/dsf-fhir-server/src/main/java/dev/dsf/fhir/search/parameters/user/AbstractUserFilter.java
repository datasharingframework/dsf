package dev.dsf.fhir.search.parameters.user;

import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.search.SearchQueryUserFilter;

abstract class AbstractUserFilter implements SearchQueryUserFilter
{
	protected final User user;
	protected final String resourceTable;
	protected final String resourceIdColumn;

	public AbstractUserFilter(User user, String resourceTable, String resourceIdColumn)
	{
		this.user = user;
		this.resourceTable = resourceTable;
		this.resourceIdColumn = resourceIdColumn;
	}
}
