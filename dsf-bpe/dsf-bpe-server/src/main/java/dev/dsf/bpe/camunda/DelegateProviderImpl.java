package dev.dsf.bpe.camunda;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;

import dev.dsf.bpe.plugin.ProcessIdAndVersion;
import dev.dsf.bpe.plugin.ProcessPlugin;

public class DelegateProviderImpl implements DelegateProvider, ProcessPluginConsumer, InitializingBean
{
	private static final class ProcessByIdAndVersion
	{
		final ProcessIdAndVersion processIdAndVersion;
		final ProcessPlugin<?, ?, ? extends TaskListener> plugin;

		ProcessByIdAndVersion(ProcessIdAndVersion idAndVersion, ProcessPlugin<?, ?, ? extends TaskListener> plugin)
		{
			this.processIdAndVersion = idAndVersion;
			this.plugin = plugin;
		}

		public ProcessIdAndVersion getProcessIdAndVersion()
		{
			return processIdAndVersion;
		}

		public ProcessPlugin<?, ?, ? extends TaskListener> getPlugin()
		{
			return plugin;
		}
	}

	private final ClassLoader defaultClassLoader;
	private final ApplicationContext defaultApplicationContext;

	private final Map<ProcessIdAndVersion, ProcessPlugin<?, ?, ? extends TaskListener>> processPluginsByIdAndVersion = new HashMap<>();

	public DelegateProviderImpl(ClassLoader mainClassLoader, ApplicationContext mainApplicationContext)
	{
		this.defaultClassLoader = mainClassLoader;
		this.defaultApplicationContext = mainApplicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(defaultClassLoader, "defaultClassLoader");
		Objects.requireNonNull(defaultApplicationContext, "defaultApplicationContext");
	}

	@Override
	public void setProcessPlugins(List<ProcessPlugin<?, ?, ? extends TaskListener>> plugins)
	{
		processPluginsByIdAndVersion.putAll(plugins.stream()
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

		var plugin = processPluginsByIdAndVersion.get(processIdAndVersion);

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

		var plugin = processPluginsByIdAndVersion.get(processIdAndVersion);

		if (plugin == null)
			return defaultApplicationContext;
		else
			return plugin.getApplicationContext();
	}

	@Override
	public Class<? extends TaskListener> getDefaultUserTaskListenerClass(String processPluginApiVersion)
	{
		return switch (processPluginApiVersion)
		{
			case "1" -> dev.dsf.bpe.v1.activity.DefaultUserTaskListener.class;
			default -> throw new IllegalArgumentException(
					"Process plugin API version " + processPluginApiVersion + " not supported");
		};
	}
}
