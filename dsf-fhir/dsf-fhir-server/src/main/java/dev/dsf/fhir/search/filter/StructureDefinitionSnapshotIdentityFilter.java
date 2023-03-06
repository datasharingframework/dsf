package dev.dsf.fhir.search.filter;

import dev.dsf.common.auth.Identity;

public class StructureDefinitionSnapshotIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final String RESOURCE_TABLE = "current_structure_definition_snapshots";
	private static final String RESOURCE_ID_COLUMN = "structure_definition_snapshot_id";

	public StructureDefinitionSnapshotIdentityFilter(Identity identity)
	{
		super(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN);
	}

	public StructureDefinitionSnapshotIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn)
	{
		super(identity, resourceTable, resourceIdColumn);
	}
}
