package dev.dsf.fhir.search.filter;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;

public class StructureDefinitionSnapshotIdentityFilter extends AbstractMetaTagAuthorizationRoleIdentityFilter
{
	private static final FhirServerRole SEARCH_ROLE = FhirServerRoleImpl.search(ResourceType.StructureDefinition);
	private static final FhirServerRole READ_ROLE = FhirServerRoleImpl.read(ResourceType.StructureDefinition);

	private static final String RESOURCE_TABLE = "current_structure_definition_snapshots";
	private static final String RESOURCE_ID_COLUMN = "structure_definition_snapshot_id";

	public StructureDefinitionSnapshotIdentityFilter(Identity identity)
	{
		this(identity, RESOURCE_TABLE, RESOURCE_ID_COLUMN, SEARCH_ROLE);
	}

	public StructureDefinitionSnapshotIdentityFilter(Identity identity, String resourceTable, String resourceIdColumn,
			FhirServerRole operationRole)
	{
		super(identity, resourceTable, resourceIdColumn, operationRole, READ_ROLE);
	}
}
