package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class ProvenanceIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_provenances";
	private static final String RESOURCE_ID_COLUMN = "provenance_id";

	public ProvenanceIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public ProvenanceIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
