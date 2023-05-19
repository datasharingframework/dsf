package dev.dsf.common.jetty;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

public final class Log4jInitializer
{
	private Log4jInitializer()
	{
	}

	// special reader code, to make sure no logger has initialized log4j
	private static Path readlog4jConfigPath()
	{
		String log4jConfig = System.getenv(JettyConfig.PROPERTY_JETTY_LOG4J_CONFIG);

		if (log4jConfig == null)
			log4jConfig = jettyProperties().getProperty(JettyConfig.PROPERTY_JETTY_LOG4J_CONFIG,
					JettyConfig.PROPERTY_JETTY_LOG4J_CONFIG_DEFAULT);

		return Paths.get(log4jConfig);
	}

	private static Properties jettyProperties()
	{
		Properties properties = new Properties();
		Path propertiesFile = Paths.get(JettyConfig.JETTY_PROPERTIES_FILE);
		if (Files.isReadable(propertiesFile))
		{
			try (Reader reader = new InputStreamReader(Files.newInputStream(propertiesFile), StandardCharsets.UTF_8))
			{
				properties.load(reader);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		return properties;
	}

	public static LoggerContext initializeLog4j()
	{
		try
		{
			Path configPath = readlog4jConfigPath();
			if (Files.isReadable(configPath))
			{
				ConfigurationSource configuration = new ConfigurationSource(Files.newInputStream(configPath),
						configPath);
				return Configurator.initialize(null, configuration);
			}
			else
			{
				LoggerContext context = Configurator.initialize(new DefaultConfiguration());
				context.getRootLogger().log(Level.ERROR, "Log4j config at " + configPath.toString()
						+ " not readable, using default logging configuration");
				return context;
			}
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
