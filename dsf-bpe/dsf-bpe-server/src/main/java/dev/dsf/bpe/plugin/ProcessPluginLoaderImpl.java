package dev.dsf.bpe.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;

public class ProcessPluginLoaderImpl implements ProcessPluginLoader, InitializingBean
{
	public static final String FILE_DRAFT_SUFFIX = "-SNAPSHOT.jar";

	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginLoaderImpl.class);

	private final Path pluginDirectory;
	private final List<ProcessPluginFactory<?, ? extends TaskListener>> processPluginFactories = new ArrayList<>();
	private final FhirContext fhirContext;
	private final ConfigurableEnvironment environment;

	public ProcessPluginLoaderImpl(
			Collection<? extends ProcessPluginFactory<?, ? extends TaskListener>> processPluginFactories,
			Path pluginDirectory, FhirContext fhirContext, ConfigurableEnvironment environment)
	{
		this.pluginDirectory = pluginDirectory;
		this.fhirContext = fhirContext;
		this.environment = environment;

		if (processPluginFactories != null)
		{
			this.processPluginFactories.addAll(processPluginFactories);
			this.processPluginFactories.sort(Comparator
					.<ProcessPluginFactory<?, ? extends TaskListener>> comparingInt(ProcessPluginFactory::getApiVersion)
					.reversed());
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(pluginDirectory, "pluginDirectory");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(environment, "environment");
	}

	@Override
	public List<ProcessPlugin<?, ?, ? extends TaskListener>> loadPlugins()
	{
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(pluginDirectory))
		{
			List<ProcessPlugin<?, ?, ? extends TaskListener>> plugins = new ArrayList<>();

			directoryStream.forEach(p ->
			{
				if (!Files.isReadable(p))
					logger.warn("Ignoring {}: {}", p.toAbsolutePath().toString(), "Not readable");
				else if (!p.getFileName().toString().endsWith(".jar"))
					logger.warn("Ignoring {}: {}", p.toAbsolutePath().toString(), "Not a .jar file");
				else
				{
					ProcessPlugin<?, ?, ? extends TaskListener> plugin = load(p);
					if (plugin != null)
						plugins.add(plugin);
				}
			});

			return plugins;
		}
		catch (IOException e)
		{
			logger.warn("Error loading process plugins", e);
			throw new RuntimeException(e);
		}
	}

	private ProcessPlugin<?, ?, ? extends TaskListener> load(Path jar)
	{
		for (ProcessPluginFactory<?, ? extends TaskListener> factory : processPluginFactories)
		{
			var plugin = load(jar, factory);

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

	private <D> ProcessPlugin<?, ?, ? extends TaskListener> load(Path jar, ProcessPluginFactory<D, ?> factory)
	{
		try
		{
			URLClassLoader classLoader = new URLClassLoader(jar.getFileName().toString(), new URL[] { toUrl(jar) },
					ClassLoader.getSystemClassLoader());

			List<Provider<D>> definitions = ServiceLoader.load(factory.getProcessPluginDefinitionType(), classLoader)
					.stream().collect(Collectors.toList());

			if (definitions.size() != 1)
				return null;

			boolean draft = jar.getFileName().toString().endsWith(FILE_DRAFT_SUFFIX);
			return factory.createProcessPlugin(definitions.get(0).get(), draft, jar, classLoader, fhirContext,
					environment);
		}
		catch (Exception e)
		{
			logger.warn("Ignoring {}: Unable to load process plugin {} - {}", jar.toString(), e.getClass().getName(),
					e.getMessage());
			return null;
		}
	}

	private URL toUrl(Path p)
	{
		try
		{
			return p.toUri().toURL();
		}
		catch (MalformedURLException e)
		{
			throw new RuntimeException(e);
		}
	}
}
