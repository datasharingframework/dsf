package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.search.SearchQueryIdentityFilter;

abstract class AbstractIdentityFilter implements SearchQueryIdentityFilter
{
	protected final Identity identity;
	protected final String resourceTable;
	protected final String resourceIdColumn;

	public AbstractIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		this.identity = identity;
		this.resourceTable = resourceTable;
		this.resourceIdColumn = resourceIdColumn;
	}
}
