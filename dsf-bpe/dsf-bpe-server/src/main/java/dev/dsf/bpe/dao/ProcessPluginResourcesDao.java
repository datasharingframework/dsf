package dev.dsf.bpe.dao;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.plugin.ProcessesResource;
import dev.dsf.bpe.plugin.ResourceInfo;

public interface ProcessPluginResourcesDao
{
	Map<ProcessIdAndVersion, List<ResourceInfo>> getResources() throws SQLException;

	void addOrRemoveResources(Collection<? extends ProcessesResource> newResources, List<UUID> deletedResourcesIds,
			List<ProcessIdAndVersion> excludedProcesses) throws SQLException;
}
