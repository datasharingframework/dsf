package dev.dsf.common.logging;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;

import dev.dsf.common.logging.Log4jConfiguration.Log4jJsonLayout.TemplateUri;
import dev.dsf.common.logging.Log4jConfiguration.Log4jLayout;

public abstract class Log4jInitializer
{
	public static final String LOG_CONFIG = "dev.dsf.log.config";
	public static final String LOG_CONFIG_DEFAULT = "conf/log4j2.xml";

	public static final String STYLE_JSON_ECS = "JSON_ECS";
	public static final String STYLE_JSON_GCP = "JSON_GCP";
	public static final String STYLE_JSON_GELF = "JSON_GELF";
	public static final String STYLE_JSON_LOGSTASH = "JSON_LOGSTASH";
	public static final String STYLE_TEXT_MDC = "TEXT_MDC";
	public static final String STYLE_TEXT = "TEXT";
	public static final String STYLE_TEXT_COLOR_MDC = "TEXT_COLOR_MDC";
	public static final String STYLE_TEXT_COLOR = "TEXT_COLOR";
	public static final String STYLE_OFF = "OFF";

	public static final String LEVEL_TRACE = "TRACE";
	public static final String LEVEL_DEBUG = "DEBUG";
	public static final String LEVEL_INFO = "INFO";
	public static final String LEVEL_WARN = "WARN";
	public static final String LEVEL_ERROR = "ERROR";

	public static final String SPECIAL_JSON_ECS = "JSON_ECS";
	public static final String SPECIAL_JSON_GCP = "JSON_GCP";
	public static final String SPECIAL_JSON_GELF = "JSON_GELF";
	public static final String SPECIAL_JSON_LOGSTASH = "JSON_LOGSTASH";
	public static final String SPECIAL_TEXT_MDC = "TEXT_MDC";
	public static final String SPECIAL_TEXT = "TEXT";
	public static final String SPECIAL_OFF = "OFF";

	public static final String LOG_FILE_STYLE = "dev.dsf.log.file.style";
	public static final String LOG_FILE_LEVEL = "dev.dsf.log.file.level";
	public static final String LOG_CONSOLE_OUT_STYLE = "dev.dsf.log.console.out.style";
	public static final String LOG_CONSOLE_OUT_LEVEL = "dev.dsf.log.console.out.level";
	public static final String LOG_CONSOLE_ERR_STYLE = "dev.dsf.log.console.err.style";
	public static final String LOG_CONSOLE_ERR_LEVEL = "dev.dsf.log.console.err.level";

	protected final Properties properties;

	protected final Log4jLayout consoleOutLayout;
	protected final Level consoleOutLevel;

	protected final Log4jLayout consoleErrLayout;
	protected final Level consoleErrLevel;

	protected final Log4jLayout fileLayout;
	protected final Level fileLevel;

	private final Path configPath;

	public Log4jInitializer()
	{
		properties = readJettyProperties();

		consoleOutLayout = getConsoleLayout(LOG_CONSOLE_OUT_STYLE, STYLE_TEXT_COLOR);
		consoleOutLevel = getLevel(LOG_CONSOLE_OUT_LEVEL, LEVEL_INFO);

		consoleErrLayout = getConsoleLayout(LOG_CONSOLE_ERR_STYLE, STYLE_OFF);
		consoleErrLevel = getLevel(LOG_CONSOLE_ERR_LEVEL, LEVEL_INFO);

		fileLayout = getFileLayout(LOG_FILE_STYLE, STYLE_TEXT_MDC);
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

	private Log4jLayout getConsoleLayout(String parameter, String defaultValue)
	{
		String value = getValue(parameter, defaultValue);

		if (STYLE_TEXT_COLOR.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jTextLayout(true);
		else if (STYLE_TEXT_COLOR_MDC.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jTextMdcLayout(true);
		else
			return getLayout(parameter, value);
	}

	private Log4jLayout getFileLayout(String parameter, String defaultValue)
	{
		String value = getValue(parameter, defaultValue);

		return getLayout(parameter, value);
	}

	private Log4jLayout getLayout(String parameter, String value)
	{
		if (STYLE_OFF.equalsIgnoreCase(value))
			return null;
		else if (STYLE_TEXT.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jTextLayout(false);
		else if (STYLE_TEXT_MDC.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jTextMdcLayout(false);
		else if (STYLE_JSON_ECS.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jJsonLayout(TemplateUri.ECS);
		else if (STYLE_JSON_GCP.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jJsonLayout(TemplateUri.GCP);
		else if (STYLE_JSON_GELF.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jJsonLayout(TemplateUri.GELF);
		else if (STYLE_JSON_LOGSTASH.equalsIgnoreCase(value))
			return new Log4jConfiguration.Log4jJsonLayout(TemplateUri.LOGSTASH);
		else
			throw new IllegalArgumentException("Value '" + value + "' for " + parameter + " not supported");
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
		else
			throw new IllegalArgumentException("Value '" + value + "' for " + parameter + " not supported");
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

	protected Function<Configuration, StringLayout> getSpecial(String parameter, String defaultValue)
	{
		String value = getValue(parameter, defaultValue);

		if (SPECIAL_JSON_ECS.equalsIgnoreCase(value))
			return configuration -> JsonTemplateLayout.newBuilder().setConfiguration(configuration)
					.setEventTemplateUri(TemplateUri.ECS.getUri()).build();
		else if (SPECIAL_JSON_GCP.equalsIgnoreCase(value))
			return configuration -> JsonTemplateLayout.newBuilder().setConfiguration(configuration)
					.setEventTemplateUri(TemplateUri.GCP.getUri()).build();
		else if (SPECIAL_JSON_GELF.equalsIgnoreCase(value))
			return configuration -> JsonTemplateLayout.newBuilder().setConfiguration(configuration)
					.setEventTemplateUri(TemplateUri.GELF.getUri()).build();
		else if (SPECIAL_JSON_LOGSTASH.equalsIgnoreCase(value))
			return configuration -> JsonTemplateLayout.newBuilder().setConfiguration(configuration)
					.setEventTemplateUri(TemplateUri.LOGSTASH.getUri()).build();
		else if (SPECIAL_TEXT.equalsIgnoreCase(value))
			return _ -> PatternLayout.newBuilder().withPattern("%d %m%n").build();
		else if (SPECIAL_TEXT_MDC.equalsIgnoreCase(value))
			return _ -> PatternLayout.newBuilder().withPattern("%d%notEmpty{ %X} %m%n").build();
		else if (SPECIAL_OFF.equalsIgnoreCase(value))
			return _ -> null;
		else
			throw new IllegalArgumentException("Value '" + value + "' for " + parameter + " not supported");
	}

	protected abstract Log4jConfigurationFactory createLog4jConfigurationFactory();

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
