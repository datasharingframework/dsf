package dev.dsf.common.logging;

import java.net.URI;
import java.util.function.BiFunction;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;

public final class Log4jConfigurationFactory extends ConfigurationFactory
{
	private final BiFunction<LoggerContext, String, Configuration> configurationFactory;

	public Log4jConfigurationFactory(BiFunction<LoggerContext, String, Configuration> configurationFactory)
	{
		this.configurationFactory = configurationFactory;
	}

	@Override
	protected String[] getSupportedTypes()
	{
		return null;
	}

	@Override
	public final Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source)
	{
		return configurationFactory.apply(loggerContext, null);
	}

	@Override
	public final Configuration getConfiguration(LoggerContext loggerContext, String name, URI configLocation,
			ClassLoader loader)
	{
		return configurationFactory.apply(loggerContext, name);
	}
}
