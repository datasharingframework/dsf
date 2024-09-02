package dev.dsf.bpe.api.plugin;

import java.util.Set;

@FunctionalInterface
public interface ProcessPluginDeploymentListener
{
	void onProcessesDeployed(Set<ProcessIdAndVersion> allActiveProcesses);
}
