package dev.dsf.bpe.plugin;

import java.util.List;

import org.camunda.bpm.engine.delegate.TaskListener;

public interface ProcessPluginLoader
{
	List<ProcessPlugin<?, ?, ? extends TaskListener>> loadPlugins();
}
