package dev.dsf.bpe.api.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public abstract class AbstractProcessPluginFactory implements ProcessPluginFactory, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractProcessPluginFactory.class);

	public static final String SNAPSHOT_FILE_SUFFIX = "-SNAPSHOT.jar";
	public static final String MILESTONE_FILE_PATTERN = ".*-M[0-9]+.jar";
	public static final String RELEASE_CANDIDATE_FILE_PATTERN = ".*-RC[0-9]+.jar";

	private final int apiVersion;
	private final ClassLoader apiClassLoader;
	protected final ApplicationContext apiApplicationContext;
	protected final ConfigurableEnvironment environment;
	private final Class<?> processPluginDefinitionType;
	private final Class<? extends TaskListener> defaultUserTaskListener;

	public AbstractProcessPluginFactory(int apiVersion, ClassLoader apiClassLoader,
			ApplicationContext apiApplicationContext, ConfigurableEnvironment environment,
			Class<?> processPluginDefinitionType, Class<? extends TaskListener> defaultUserTaskListener)
	{
		this.apiVersion = apiVersion;
		this.apiClassLoader = apiClassLoader;
		this.apiApplicationContext = apiApplicationContext;
		this.environment = environment;
		this.processPluginDefinitionType = processPluginDefinitionType;
		this.defaultUserTaskListener = defaultUserTaskListener;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(apiClassLoader, "apiClassLoader");
		Objects.requireNonNull(apiApplicationContext, "apiApplicationContext");
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(processPluginDefinitionType, "processPluginDefinitionType");
		Objects.requireNonNull(defaultUserTaskListener, "defaultUserTaskListener");
	}

	@Override
	public int getApiVersion()
	{
		return apiVersion;
	}

	@Override
	public Class<? extends TaskListener> getDefaultUserTaskListener()
	{
		return defaultUserTaskListener;
	}

	public ProcessPlugin load(Path jar)
	{
		try
		{
			URLClassLoader pluginClassLoader = new URLClassLoader(jar.getFileName().toString(),
					new URL[] { toUrl(jar) }, apiClassLoader);

			List<Provider<?>> definitions = ServiceLoader.load(processPluginDefinitionType, pluginClassLoader).stream()
					.collect(Collectors.toList());

			if (definitions.size() != 1)
				return null;

			String filename = jar.getFileName().toString();
			boolean isSnapshot = filename.endsWith(SNAPSHOT_FILE_SUFFIX);
			boolean isMilestone = filename.matches(MILESTONE_FILE_PATTERN);
			boolean isReleaseCandidate = filename.matches(RELEASE_CANDIDATE_FILE_PATTERN);

			boolean draft = isSnapshot || isMilestone || isReleaseCandidate;

			return createProcessPlugin(definitions.get(0).get(), draft, jar, pluginClassLoader);
		}
		catch (Exception e)
		{
			logger.debug("Ignoring {}: Unable to load process plugin", jar.toString(), e);
			logger.warn("Ignoring {}: Unable to load process plugin: {} - {}", jar.toString(), e.getClass().getName(),
					e.getMessage());

			return null;
		}
	}

	protected abstract ProcessPlugin createProcessPlugin(Object processPluginDefinition, boolean draft, Path jarFile,
			URLClassLoader pluginClassLoader);

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
