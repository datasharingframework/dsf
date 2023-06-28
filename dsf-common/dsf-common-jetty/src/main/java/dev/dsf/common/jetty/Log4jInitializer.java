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
	public static final String LOG4J_CONFIG = "dev.dsf.log4j.config";
	public static final String LOG4J_CONFIG_DEFAULT = "conf/log4j2.xml";

	private Log4jInitializer()
	{
	}

	// special reader code, to make sure no logger has initialized log4j
	private static Path readlog4jConfigPath()
	{
		String log4jConfig = System.getenv(LOG4J_CONFIG.replace(".", "_"));

		if (log4jConfig == null)
			log4jConfig = jettyProperties().getProperty(LOG4J_CONFIG, LOG4J_CONFIG_DEFAULT);

		return Paths.get(log4jConfig);
	}

	private static Properties jettyProperties()
	{
		Properties properties = new Properties();
		Path propertiesFile = Paths.get("conf/jetty.properties");
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
