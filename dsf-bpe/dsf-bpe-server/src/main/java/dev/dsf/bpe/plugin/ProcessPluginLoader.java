package dev.dsf.bpe.plugin;

import java.util.List;

public interface ProcessPluginLoader
{
	List<ProcessPlugin<?, ?>> loadPlugins();
}
