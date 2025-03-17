package dev.dsf.bpe.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class ProcessPluginApiClassLoaderFactory implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginApiClassLoaderFactory.class);

	private static final String ALLOWED_BPE_CLASSES_LIST = "allowed-bpe-classes.list";
	private static final String RESOURCES_WITH_PRIORITY_LIST = "resources-with-priority.list";
	private static final String ALLOWED_BPE_RESOURCES = "allowed-bpe-resources.list";

	private final Path apiClassPathBaseDirectory;

	private final Map<Integer, Path> allowedBpeClasses = new HashMap<>();
	private final Map<Integer, Path> resourcesWithPriority = new HashMap<>();
	private final Map<Integer, Path> allowedBpeResources = new HashMap<>();

	public ProcessPluginApiClassLoaderFactory(Path apiClassPathBaseDirectory, Map<Integer, Path> allowedBpeClasses,
			Map<Integer, Path> resourcesWithPriority, Map<Integer, Path> allowedBpeResources)
	{
		this.apiClassPathBaseDirectory = apiClassPathBaseDirectory;

		if (allowedBpeClasses != null)
			this.allowedBpeClasses.putAll(allowedBpeClasses);
		if (resourcesWithPriority != null)
			this.resourcesWithPriority.putAll(resourcesWithPriority);
		if (allowedBpeResources != null)
			this.allowedBpeResources.putAll(allowedBpeResources);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(apiClassPathBaseDirectory, "apiClassPathBaseDirectory");
		// list files may be null
	}

	private List<Path> getApiClassPath(int apiVersion)
	{
		Path apiClassPathDirectory = apiClassPathBaseDirectory.resolve("v" + apiVersion);

		try
		{
			List<Path> files = Files.list(apiClassPathDirectory)
					.filter(p -> p.getFileName().toString().endsWith(".jar")).toList();

			if (files.isEmpty())
				throw new IllegalArgumentException("No jar files found for API v" + apiVersion + " class-path at "
						+ apiClassPathDirectory.toAbsolutePath().normalize().toString());

			return files;
		}
		catch (IOException e)
		{
			logger.warn("Unable to iterate files in api class path directory {}", apiClassPathDirectory);
			throw new RuntimeException(
					"Unable to iterate files in api class path directory " + apiClassPathDirectory.toString(), e);
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

	private Set<String> readList(int apiVersion, String file)
	{
		Path externalFile = getExternalFileIfReadable(apiVersion, file);
		return externalFile == null ? readInternal(apiVersion, file) : readExternal(apiVersion, file, externalFile);
	}

	private Path getExternalFileIfReadable(int apiVersion, String file)
	{
		Path externalFile = switch (file)
		{
			case ALLOWED_BPE_CLASSES_LIST -> allowedBpeClasses.get(apiVersion);
			case RESOURCES_WITH_PRIORITY_LIST -> resourcesWithPriority.get(apiVersion);
			case ALLOWED_BPE_RESOURCES -> allowedBpeResources.get(apiVersion);

			default -> throw new IllegalArgumentException("Unexpected file value: " + file);
		};

		if (externalFile == null)
		{
			logger.debug("External file for api v{} not configured, using {} from jar", apiVersion, file);
			return null;
		}

		if (!Files.exists(externalFile))
		{
			logger.debug("External file for api v{} {} does not exist, using {} from jar", apiVersion,
					externalFile.toAbsolutePath().normalize().toString(), file);
			return null;
		}

		if (!Files.isReadable(externalFile))
		{
			logger.debug("External file for api v{} {} is not readable, using {} from jar", apiVersion,
					externalFile.toAbsolutePath().normalize().toString(), file);
			return null;
		}

		return externalFile;
	}

	private Set<String> readExternal(int apiVersion, String file, Path externalFile)
	{
		try
		{
			logger.debug("Reading api v{} file {} from {} ...", apiVersion, file,
					externalFile.toAbsolutePath().normalize().toString());
			return new HashSet<>(Files.readAllLines(externalFile));
		}
		catch (IOException e)
		{
			logger.warn("Unable to read api v{} file {} from external file {}", apiVersion, file,
					externalFile.toAbsolutePath().normalize().toString());
			throw new RuntimeException(
					"Unable to read external file " + externalFile.toAbsolutePath().normalize().toString(), e);
		}
	}

	private Set<String> readInternal(int apiVersion, String file)
	{
		final String path = "bpe/api/v" + apiVersion + "/" + file;

		try (InputStream in = ProcessPluginApiClassLoaderFactory.class.getClassLoader().getResourceAsStream(path);
				InputStreamReader inReader = new InputStreamReader(in, StandardCharsets.UTF_8);
				BufferedReader reader = new BufferedReader(inReader))
		{
			List<String> result = new ArrayList<>();
			for (;;)
			{
				String line = reader.readLine();
				if (line == null)
					break;
				result.add(line);
			}
			return new HashSet<>(result);
		}
		catch (IOException e)
		{
			logger.warn("Unable to read internal file {}", path);
			throw new RuntimeException("Unable to read internal file " + path, e);
		}
	}

	public ProcessPluginApiClassLoader createApiClassLoader(int apiVersion)
	{
		List<Path> apiClassPath = getApiClassPath(apiVersion);

		Set<String> allowedBpeClasses = readList(apiVersion, ALLOWED_BPE_CLASSES_LIST);
		Set<String> resourcesWithPriority = readList(apiVersion, RESOURCES_WITH_PRIORITY_LIST);
		Set<String> allowedBpeResources = readList(apiVersion, ALLOWED_BPE_RESOURCES);

		logger.debug("Creating Plugin API class loader for v{} with jar files from {}: {}", apiVersion,
				apiClassPathBaseDirectory.resolve("v" + apiVersion).toAbsolutePath().normalize(), apiClassPath.stream()
						.map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", ", "[", "]")));

		return new ProcessPluginApiClassLoader("Plugin API v" + apiVersion,
				apiClassPath.stream().map(this::toUrl).toArray(URL[]::new), ClassLoader.getSystemClassLoader(),
				allowedBpeClasses, resourcesWithPriority, allowedBpeResources);
	}
}
