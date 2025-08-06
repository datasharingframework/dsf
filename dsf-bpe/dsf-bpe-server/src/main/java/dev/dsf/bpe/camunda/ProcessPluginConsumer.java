package dev.dsf.bpe.camunda;

import java.util.List;
import java.util.Map;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;

public interface ProcessPluginConsumer
{
	void setProcessPlugins(List<ProcessPlugin> plugins,
			Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion);
}
