package dev.dsf.bpe.camunda;

import java.util.List;

import dev.dsf.bpe.plugin.ProcessPlugin;

public interface ProcessPluginConsumer
{
	void setProcessPlugins(List<ProcessPlugin<?, ?>> plugins);
}
