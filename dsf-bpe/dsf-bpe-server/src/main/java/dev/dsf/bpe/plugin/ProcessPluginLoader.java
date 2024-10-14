package dev.dsf.bpe.plugin;

import java.util.List;

import dev.dsf.bpe.api.plugin.ProcessPlugin;

public interface ProcessPluginLoader
{
	List<ProcessPlugin> loadPlugins();
}
