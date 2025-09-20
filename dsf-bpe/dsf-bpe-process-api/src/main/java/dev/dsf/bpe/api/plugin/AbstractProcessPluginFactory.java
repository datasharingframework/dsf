package dev.dsf.bpe.api.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

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
	protected final String serverBaseUrl;
	private final Class<?> processPluginDefinitionType;

	public AbstractProcessPluginFactory(int apiVersion, ClassLoader apiClassLoader,
			ApplicationContext apiApplicationContext, ConfigurableEnvironment environment, String serverBaseUrl,
			Class<?> processPluginDefinitionType)
	{
		this.apiVersion = apiVersion;
		this.apiClassLoader = apiClassLoader;
		this.apiApplicationContext = apiApplicationContext;
		this.environment = environment;
		this.serverBaseUrl = serverBaseUrl;
		this.processPluginDefinitionType = processPluginDefinitionType;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(apiClassLoader, "apiClassLoader");
		Objects.requireNonNull(apiApplicationContext, "apiApplicationContext");
		Objects.requireNonNull(environment, "environment");
		Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
		Objects.requireNonNull(processPluginDefinitionType, "processPluginDefinitionType");
	}

	@Override
	public int getApiVersion()
	{
		return apiVersion;
	}

	@Override
	public ProcessPlugin load(Path pluginPath)
	{
		try
		{
			URLClassLoader pluginClassLoader = new URLClassLoader(pluginPath.getFileName().toString(),
					new URL[] { toUrl(pluginPath) }, apiClassLoader);

			List<Provider<?>> definitions = ServiceLoader.load(processPluginDefinitionType, pluginClassLoader).stream()
					.collect(Collectors.toList());

			if (definitions.size() < 1)
				return null;
			else if (definitions.size() > 1)
			{
				logger.warn("Ignoring {}: {} process plugin definition classes for API version {} found",
						pluginPath.toString(), definitions.size(), apiVersion);
				return null;
			}

			String filename = pluginPath.getFileName().toString();
			boolean isSnapshot = filename.endsWith(SNAPSHOT_FILE_SUFFIX);
			boolean isMilestone = filename.matches(MILESTONE_FILE_PATTERN);
			boolean isReleaseCandidate = filename.matches(RELEASE_CANDIDATE_FILE_PATTERN);

			boolean draft = isSnapshot || isMilestone || isReleaseCandidate;

			return createProcessPlugin(definitions.get(0).get(), draft, pluginPath, pluginClassLoader);
		}
		catch (ServiceConfigurationError | Exception e)
		{
			logger.debug("Ignoring {}: Unable to load process plugin", pluginPath.toString(), e);
			logger.warn("Ignoring {}: Unable to load process plugin: {} - {}", pluginPath.toString(),
					e.getClass().getName(), e.getMessage());

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
