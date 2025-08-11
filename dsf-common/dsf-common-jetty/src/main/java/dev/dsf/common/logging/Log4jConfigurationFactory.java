package dev.dsf.common.logging;

import java.net.URI;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;

import dev.dsf.common.logging.Log4jConfiguration.Log4jLayout;

public class Log4jConfigurationFactory extends ConfigurationFactory
{
	protected final String fileNamePart;
	protected final Log4jLayout consoleLayout;
	protected final Level consoleLevel;
	protected final Log4jLayout fileLayout;
	protected final Level fileLevel;

	public Log4jConfigurationFactory(String fileNamePart, Log4jLayout consoleLayout, Level consoleLevel,
			Log4jLayout fileLayout, Level fileLevel)
	{
		this.fileNamePart = fileNamePart;
		this.consoleLayout = consoleLayout;
		this.consoleLevel = consoleLevel;
		this.fileLayout = fileLayout;
		this.fileLevel = fileLevel;
	}

	@Override
	protected String[] getSupportedTypes()
	{
		return null;
	}

	protected Log4jConfiguration doGetConfiguration(LoggerContext loggerContext, String name)
	{
		return new Log4jConfiguration(loggerContext, name, fileNamePart, consoleLayout, consoleLevel, fileLayout,
				fileLevel);
	}

	@Override
	public final Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source)
	{
		return doGetConfiguration(loggerContext, null);
	}

	@Override
	public final Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation,
			ClassLoader loader)
	{
		return doGetConfiguration(loggerContext, name);
	}
}
