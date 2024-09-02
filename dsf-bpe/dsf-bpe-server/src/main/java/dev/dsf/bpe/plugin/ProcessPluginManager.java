package dev.dsf.bpe.plugin;

import java.util.Optional;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;

public interface ProcessPluginManager
{
	void loadAndDeployPlugins();

	Optional<ProcessPlugin> getProcessPlugin(ProcessIdAndVersion processIdAndVersion);
}
