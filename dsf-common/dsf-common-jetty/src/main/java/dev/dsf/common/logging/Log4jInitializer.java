package dev.dsf.common.logging;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import dev.dsf.common.logging.Log4jConfiguration.Log4jJsonLayout.TemplateUri;
import dev.dsf.common.logging.Log4jConfiguration.Log4jLayout;

public class Log4jInitializer
{
	public static final String LOG_CONFIG = "dev.dsf.log.config";
	public static final String LOG_CONFIG_DEFAULT = "conf/log4j2.xml";

	public static final String STYLE_JSON_ECS = "JSON_ECS";
	public static final String STYLE_JSON_GCP = "JSON_GCP";
	public static final String STYLE_JSON_GELF = "JSON_GELF";
	public static final String STYLE_JSON_LOGSTASH = "JSON_LOGSTASH";
	public static final String STYLE_TEXT_MDC = "TEXT_MDC";
	public static final String STYLE_TEXT = "TEXT";

	public static final String LEVEL_TRACE = "TRACE";
	public static final String LEVEL_DEBUG = "DEBUG";
	public static final String LEVEL_INFO = "INFO";
	public static final String LEVEL_WARN = "WARN";
	public static final String LEVEL_ERROR = "ERROR";
	public static final String LEVEL_OFF = "OFF";

	public static final String LOG_FILE_STYLE = "dev.dsf.log.file.style";
	public static final String LOG_FILE_LEVEL = "dev.dsf.log.file.level";
	public static final String LOG_CONSOLE_STYLE = "dev.dsf.log.console.style";
	public static final String LOG_CONSOLE_LEVEL = "dev.dsf.log.console.level";

	protected final String fileNamePart;

	protected final Properties properties;

	protected final Log4jLayout consoleLayout;
	protected final Level consoleLevel;
	protected final Log4jLayout fileLayout;
	protected final Level fileLevel;

	private final Path configPath;

	public Log4jInitializer(String fileNamePart)
	{
		this.fileNamePart = Objects.requireNonNull(fileNamePart, "fileNamePart");

		properties = readJettyProperties();

		consoleLayout = getLayout(LOG_CONSOLE_STYLE, STYLE_TEXT);
		consoleLevel = getLevel(LOG_CONSOLE_LEVEL, LEVEL_INFO);
		fileLayout = getLayout(LOG_FILE_STYLE, STYLE_TEXT_MDC);
		fileLevel = getLevel(LOG_FILE_LEVEL, LEVEL_DEBUG);

		configPath = getConfigPath(LOG_CONFIG, LOG_CONFIG_DEFAULT);
	}

	private static Properties readJettyProperties()
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

	private Log4jLayout getLayout(String parameter, String defaultValue)
	{
		String value = getValue(parameter, defaultValue);

		if (STYLE_TEXT.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jTextLayout();
		else if (STYLE_TEXT_MDC.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jTextMdcLayout();
		else if (STYLE_JSON_ECS.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jJsonLayout(TemplateUri.ECS);
		else if (STYLE_JSON_GCP.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jJsonLayout(TemplateUri.GCP);
		else if (STYLE_JSON_GELF.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jJsonLayout(TemplateUri.GELF);
		else if (STYLE_JSON_LOGSTASH.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jJsonLayout(TemplateUri.LOGSTASH);
		else
			throw new IllegalArgumentException("Log style '" + value + "' for " + parameter + " not supported");
	}

	private Level getLevel(String parameter, String defaultValue)
	{
		String value = getValue(parameter, defaultValue);

		if (LEVEL_TRACE.equalsIgnoreCase(value))
			return Level.TRACE;
		else if (LEVEL_DEBUG.equalsIgnoreCase(value))
			return Level.DEBUG;
		else if (LEVEL_INFO.equalsIgnoreCase(value))
			return Level.INFO;
		else if (LEVEL_WARN.equalsIgnoreCase(value))
			return Level.WARN;
		else if (LEVEL_ERROR.equalsIgnoreCase(value))
			return Level.ERROR;
		else if (LEVEL_OFF.equalsIgnoreCase(value))
			return Level.OFF;
		else
			throw new IllegalArgumentException("Log level '" + value + "' for " + parameter + " not supported");
	}

	private Path getConfigPath(String parameter, String defaultValue)
	{
		String value = getValue(parameter, defaultValue);

		return Paths.get(value);
	}

	protected String getValue(String parameter, String defaultValue)
	{
		String value = System.getenv(parameter.replace(".", "_").toUpperCase(Locale.ENGLISH));

		if (value == null)
			value = properties.getProperty(parameter, defaultValue);

		return value;
	}

	protected Log4jConfigurationFactory createLog4jConfigurationFactory()
	{
		return new Log4jConfigurationFactory(fileNamePart, consoleLayout, consoleLevel, fileLayout, fileLevel);
	}

	public void initializeLog4j()
	{
		try
		{
			if (Files.isReadable(configPath))
				Configurator.initialize(null, new ConfigurationSource(Files.newInputStream(configPath), configPath));
			else
				ConfigurationFactory.setConfigurationFactory(createLog4jConfigurationFactory());
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}
}
