package dev.dsf.bpe.camunda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;

public class DelegateProviderImpl implements DelegateProvider, ProcessPluginConsumer, InitializingBean
{
	private static final class ProcessByIdAndVersion
	{
		final ProcessIdAndVersion processIdAndVersion;
		final ProcessPlugin plugin;

		ProcessByIdAndVersion(ProcessIdAndVersion idAndVersion, ProcessPlugin plugin)
		{
			this.processIdAndVersion = idAndVersion;
			this.plugin = plugin;
		}

		public ProcessIdAndVersion getProcessIdAndVersion()
		{
			return processIdAndVersion;
		}

		public ProcessPlugin getPlugin()
		{
			return plugin;
		}
	}

	private final ClassLoader defaultClassLoader;
	private final ApplicationContext defaultApplicationContext;

	private final Map<ProcessIdAndVersion, ProcessPlugin> processPluginsByProcessIdAndVersion = new HashMap<>();
	private final Map<String, Class<? extends TaskListener>> defaultUserTaskListenerByApiVersion;

	public DelegateProviderImpl(ClassLoader mainClassLoader, ApplicationContext mainApplicationContext,
			List<ProcessPluginFactory> pluginFactories)
	{
		this.defaultClassLoader = mainClassLoader;
		this.defaultApplicationContext = mainApplicationContext;

		Objects.requireNonNull(pluginFactories, "pluginFactories");

		defaultUserTaskListenerByApiVersion = pluginFactories.stream().collect(Collectors
				.toMap(f -> String.valueOf(f.getApiVersion()), ProcessPluginFactory::getDefaultUserTaskListener));
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(defaultClassLoader, "defaultClassLoader");
		Objects.requireNonNull(defaultApplicationContext, "defaultApplicationContext");
	}

	@Override
	public void setProcessPlugins(List<ProcessPlugin> plugins)
	{
		processPluginsByProcessIdAndVersion.putAll(plugins.stream()
				.flatMap(plugin -> plugin.getProcessKeysAndVersions().stream()
						.map(idAndVersion -> new ProcessByIdAndVersion(idAndVersion, plugin)))
				.collect(Collectors.toMap(ProcessByIdAndVersion::getProcessIdAndVersion,
						ProcessByIdAndVersion::getPlugin)));
	}

	@Override
	public ClassLoader getClassLoader(ProcessIdAndVersion processIdAndVersion)
	{
		if (processIdAndVersion == null)
			return defaultClassLoader;

		var plugin = processPluginsByProcessIdAndVersion.get(processIdAndVersion);

		if (plugin == null)
			return defaultClassLoader;
		else
			return plugin.getProcessPluginClassLoader();
	}

	@Override
	public ApplicationContext getApplicationContext(ProcessIdAndVersion processIdAndVersion)
	{
		if (processIdAndVersion == null)
			return defaultApplicationContext;

		var plugin = processPluginsByProcessIdAndVersion.get(processIdAndVersion);

		if (plugin == null)
			return defaultApplicationContext;
		else
			return plugin.getApplicationContext();
	}

	@Override
	public Class<? extends TaskListener> getDefaultUserTaskListenerClass(String processPluginApiVersion)
	{
		Class<? extends TaskListener> listenerClass = defaultUserTaskListenerByApiVersion.get(processPluginApiVersion);

		if (listenerClass != null)
			return listenerClass;
		else
			throw new IllegalArgumentException(
					"Process plugin api version " + processPluginApiVersion + " not supported");
	}
}
