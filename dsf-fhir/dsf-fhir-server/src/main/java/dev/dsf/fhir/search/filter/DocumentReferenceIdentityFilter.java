package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class DocumentReferenceIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_document_references";
	private static String RESOURCE_ID_COLUMN = "document_reference_id";

	public DocumentReferenceIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public DocumentReferenceIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
