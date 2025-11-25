/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
