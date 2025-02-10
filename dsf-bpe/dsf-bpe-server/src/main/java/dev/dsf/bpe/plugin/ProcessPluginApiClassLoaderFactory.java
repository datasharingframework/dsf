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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessPluginApiClassLoaderFactory
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginApiClassLoaderFactory.class);

	private static final String ALLOWED_BPE_CLASSES_LIST = "allowed-bpe-classes.list";
	private static final String API_RESOURCES_WITH_PRIORITY_LIST = "api-resources-with-priority.list";
	private static final String ALLOWED_BPE_RESOURCES = "allowed-bpe-resources.list";

	private URL[] getApiClassPath(String apiVersion)
	{
		Path apiClassPathFolder = Paths.get("api/v" + apiVersion);

		try
		{
			return Files.list(apiClassPathFolder).filter(p -> p.getFileName().toString().endsWith(".jar"))
					.map(this::toUrl).toArray(URL[]::new);
		}
		catch (IOException e)
		{
			logger.warn("Unable to iterate files in api class path folder {}", apiClassPathFolder);
			throw new RuntimeException(
					"Unable to iterate files in api class path folder " + apiClassPathFolder.toString(), e);
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

	private Set<String> readList(String apiVersion, String file)
	{
		Path externalFile = getExternalFileIfReadable(apiVersion, file);
		return externalFile == null ? readInternal(apiVersion, file) : readExternal(externalFile);
	}

	private Path getExternalFileIfReadable(String apiVersion, String file)
	{
		Path externalFile = Paths.get("bpe/api/v" + apiVersion + "/" + file);

		if (!Files.exists(externalFile))
		{
			logger.debug("External file {} does not exist, using file from jar",
					externalFile.toAbsolutePath().toString());
			return null;
		}

		if (!Files.isReadable(externalFile))
		{
			logger.debug("External file {} is not readable, using file from jar",
					externalFile.toAbsolutePath().toString());
			return null;
		}

		return externalFile;
	}

	private Set<String> readExternal(Path file)
	{
		try
		{
			logger.debug("Reading {} ...", file.toAbsolutePath().toString());
			return new HashSet<>(Files.readAllLines(file));
		}
		catch (IOException e)
		{
			logger.warn("Unable to read external file {}", file.toAbsolutePath().toString());
			throw new RuntimeException("Unable to read external file " + file.toAbsolutePath().toString(), e);
		}
	}

	private Set<String> readInternal(String apiVersion, String file)
	{
		final String path = "api/v" + apiVersion + "/" + file;

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

	public ProcessPluginApiClassLoader createApiClassLoader(String apiVersion)
	{
		URL[] apiClassPath = getApiClassPath(apiVersion);

		Set<String> allowedBpeClasses = readList(apiVersion, ALLOWED_BPE_CLASSES_LIST);
		Set<String> apiResourcesWithPriority = readList(apiVersion, API_RESOURCES_WITH_PRIORITY_LIST);
		Set<String> allowedBpeResources = readList(apiVersion, ALLOWED_BPE_RESOURCES);

		return new ProcessPluginApiClassLoader("Plugin API v" + apiVersion, apiClassPath,
				ClassLoader.getSystemClassLoader(), allowedBpeClasses, apiResourcesWithPriority, allowedBpeResources);
	}
}
