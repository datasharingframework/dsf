package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class StructureDefinitionIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_structure_definitions";
	private static final String RESOURCE_ID_COLUMN = "structure_definition_id";

	public StructureDefinitionIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public StructureDefinitionIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
