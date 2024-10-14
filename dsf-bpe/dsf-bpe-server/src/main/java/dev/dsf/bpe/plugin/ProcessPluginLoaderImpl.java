package dev.dsf.bpe.plugin;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;

public class ProcessPluginLoaderImpl implements ProcessPluginLoader, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginLoaderImpl.class);

	private final Path pluginDirectory;
	private final List<ProcessPluginFactory> processPluginFactories = new ArrayList<>();

	public ProcessPluginLoaderImpl(Collection<? extends ProcessPluginFactory> processPluginFactories,
			Path pluginDirectory)
	{
		this.pluginDirectory = pluginDirectory;

		if (processPluginFactories != null)
		{
			this.processPluginFactories.addAll(processPluginFactories);
			this.processPluginFactories.sort(
					Comparator.<ProcessPluginFactory> comparingInt(ProcessPluginFactory::getApiVersion).reversed());
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(pluginDirectory, "pluginDirectory");
	}

	@Override
	public List<ProcessPlugin> loadPlugins()
	{
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(pluginDirectory))
		{
			List<ProcessPlugin> plugins = new ArrayList<>();

			directoryStream.forEach(p ->
			{
				if (!Files.isReadable(p))
					logger.warn("Ignoring {}: {}", p.toAbsolutePath().toString(), "Not readable");
				else if (!p.getFileName().toString().endsWith(".jar"))
					logger.warn("Ignoring {}: {}", p.toAbsolutePath().toString(), "Not a .jar file");
				else
				{
					ProcessPlugin plugin = load(p);
					if (plugin != null)
						plugins.add(plugin);
				}
			});

			return plugins;
		}
		catch (IOException e)
		{
			logger.debug("Error loading process plugins", e);
			logger.warn("Error loading process plugins: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}

	private ProcessPlugin load(Path jar)
	{
		for (ProcessPluginFactory factory : processPluginFactories)
		{
			ProcessPlugin plugin = factory.load(jar);

			if (plugin != null)
				return plugin;
		}

		logger.warn("Ignoring {}: No process plugin definition for API version{} {} found", jar.toString(),
				processPluginFactories.size() != 1 ? "s" : "",
				processPluginFactories.size() == 1 ? processPluginFactories.get(0).getApiVersion()
						: processPluginFactories.stream().map(f -> String.valueOf(f.getApiVersion()))
								.collect(Collectors.joining(", ", "[", "]")));
		return null;
	}
}
