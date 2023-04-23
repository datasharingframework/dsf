package dev.dsf.bpe.camunda;

import java.util.List;

import org.camunda.bpm.engine.delegate.TaskListener;

import dev.dsf.bpe.plugin.ProcessPlugin;

public interface ProcessPluginConsumer
{
	void setProcessPlugins(List<ProcessPlugin<?, ?, ? extends TaskListener>> plugins);
}
