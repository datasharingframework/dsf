package dev.dsf.bpe.dao;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.dsf.bpe.process.ProcessKeyAndVersion;
import dev.dsf.bpe.process.ProcessesResource;
import dev.dsf.bpe.process.ResourceInfo;

public interface ProcessPluginResourcesDao
{
	Map<ProcessKeyAndVersion, List<ResourceInfo>> getResources() throws SQLException;

	void addOrRemoveResources(Collection<? extends ProcessesResource> newResources, List<UUID> deletedResourcesIds,
			List<ProcessKeyAndVersion> excludedProcesses) throws SQLException;
}
