package dev.dsf.bpe.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.PropertyResolver;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.ProcessPluginDefinition;
import dev.dsf.bpe.process.ProcessKeyAndVersion;
import dev.dsf.bpe.process.ProcessState;
import dev.dsf.bpe.process.ProcessStateChangeOutcome;

public class ProcessPluginProviderImpl implements ProcessPluginProvider, InitializingBean
{
	private static final class Pair<K, V>
	{
		final K k;
		final V v;

		Pair(K k, V v)
		{
			this.k = k;
			this.v = v;
		}
	}

	public static final String FILE_DRAFT_SUFFIX = "-SNAPSHOT.jar";

	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginProviderImpl.class);

	private final FhirContext fhirContext;
	private final Path pluginDirectory;
	private final ApplicationContext mainApplicationContext;
	private final PropertyResolver resolver;

	private final List<ProcessPluginDefinitionAndClassLoader> definitions;

	public ProcessPluginProviderImpl(FhirContext fhirContext, Path pluginDirectory,
			ApplicationContext mainApplicationContext, PropertyResolver resolver)
	{
		this.fhirContext = fhirContext;
		this.pluginDirectory = pluginDirectory;
		this.mainApplicationContext = mainApplicationContext;
		this.resolver = resolver;

		definitions = loadDefinitions();
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(pluginDirectory, "pluginDirectory");
		Objects.requireNonNull(mainApplicationContext, "mainApplicationContext");
	}

	@Override
	public List<ProcessPluginDefinitionAndClassLoader> getDefinitions()
	{
		return definitions;
	}

	private List<ProcessPluginDefinitionAndClassLoader> loadDefinitions()
	{
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(pluginDirectory))
		{
			List<ProcessPluginDefinitionAndClassLoader> definitions = new ArrayList<>();

			directoryStream.forEach(p ->
			{
				if (Files.isReadable(p) && p.getFileName().toString().endsWith(".jar"))
				{
					ProcessPluginDefinitionAndClassLoader def = toDefinition(p);
					if (def != null)
						definitions.add(def);
				}
				else
					logger.warn("Ignoring file/folder {}", p.toAbsolutePath().toString());
			});

			return definitions;
		}
		catch (IOException e)
		{
			logger.warn("Error loading process plugin definitions", e);
			throw new RuntimeException(e);
		}
	}

	private ProcessPluginDefinitionAndClassLoader toDefinition(Path jar)
	{
		URLClassLoader classLoader = new URLClassLoader(jar.getFileName().toString(), new URL[] { toUrl(jar) },
				ClassLoader.getSystemClassLoader());

		List<Provider<ProcessPluginDefinition>> definitions = ServiceLoader
				.load(ProcessPluginDefinition.class, classLoader).stream().collect(Collectors.toList());

		if (definitions.size() < 1)
		{
			logger.warn("Ignoring {}: no {} found", jar.toString(), ProcessPluginDefinition.class.getName());
			return null;
		}
		else if (definitions.size() > 1)
		{
			logger.warn("Ignoring {}: more than one {} found", jar.toString(), ProcessPluginDefinition.class.getName());
			return null;
		}

		boolean draft = jar.getFileName().toString().endsWith(FILE_DRAFT_SUFFIX);

		return new ProcessPluginDefinitionAndClassLoader(fhirContext, jar, definitions.get(0).get(), classLoader, draft,
				resolver);
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

	@Override
	public Map<ProcessKeyAndVersion, ClassLoader> getClassLoadersByProcessDefinitionKeyAndVersion()
	{
		return getDefinitions().stream()
				.flatMap(def -> def.getProcessKeysAndVersions().stream()
						.map(keyAndVersion -> new Pair<>(keyAndVersion, def.getClassLoader())))
				.collect(Collectors.toMap(p -> p.k, p -> p.v, duplicatedProcessKeyVersion()));
	}

	private <T> BinaryOperator<T> duplicatedProcessKeyVersion()
	{
		return (v1, v2) ->
		{
			throw new RuntimeException("duplicate processes, check process keys/versions");
		};
	}

	@Override
	public Map<ProcessKeyAndVersion, ApplicationContext> getApplicationContextsByProcessDefinitionKeyAndVersion()
	{
		return getDefinitions().stream().flatMap(def -> def.getProcessKeysAndVersions().stream().map(
				keyAndVersion -> new Pair<>(keyAndVersion, def.getPluginApplicationContext(mainApplicationContext))))
				.collect(Collectors.toMap(p -> p.k, p -> p.v, duplicatedProcessKeyVersion()));
	}

	@Override
	public Map<ProcessKeyAndVersion, ProcessPluginDefinitionAndClassLoader> getDefinitionByProcessKeyAndVersion()
	{
		return getDefinitions().stream().flatMap(
				def -> def.getProcessKeysAndVersions().stream().map(keyAndVersion -> new Pair<>(keyAndVersion, def)))
				.collect(Collectors.toMap(p -> p.k, p -> p.v, duplicatedProcessKeyVersion()));
	}

	@Override
	public List<ProcessKeyAndVersion> getProcessKeyAndVersions()
	{
		return getDefinitions().stream().flatMap(def -> def.getProcessKeysAndVersions().stream())
				.collect(Collectors.toList());
	}

	@Override
	public List<ProcessKeyAndVersion> getDraftProcessKeyAndVersions()
	{
		return getDefinitions().stream().filter(ProcessPluginDefinitionAndClassLoader::isDraft)
				.flatMap(def -> def.getProcessKeysAndVersions().stream()).collect(Collectors.toList());
	}

	@Override
	public void onProcessesDeployed(List<ProcessStateChangeOutcome> changes)
	{
		Set<ProcessKeyAndVersion> activeProcesses = changes.stream()
				.filter(c -> EnumSet.of(ProcessState.ACTIVE, ProcessState.DRAFT).contains(c.getNewProcessState()))
				.map(ProcessStateChangeOutcome::getProcessKeyAndVersion).collect(Collectors.toCollection(HashSet::new));

		for (ProcessPluginDefinitionAndClassLoader definition : getDefinitions())
		{
			List<String> pluginActiveProcesses = definition.getProcessKeysAndVersions().stream()
					.filter(activeProcesses::contains).map(ProcessKeyAndVersion::getKey).sorted()
					.collect(Collectors.toList());

			ApplicationContext pluginApplicationContext = definition
					.getPluginApplicationContext(mainApplicationContext);

			try
			{
				definition.getDefinition().onProcessesDeployed(pluginApplicationContext, pluginActiveProcesses);
			}
			catch (Exception e)
			{
				logger.warn("Error while executing onProcessesDeployed for plugin "
						+ definition.getDefinition().getName() + "-" + definition.getDefinition().getVersion(), e);
			}
		}
	}
}
