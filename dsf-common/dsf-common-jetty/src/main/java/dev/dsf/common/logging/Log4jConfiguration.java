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

import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.OnStartupTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.layout.template.json.JsonTemplateLayout;

public class Log4jConfiguration extends AbstractConfiguration
{
	public interface Log4jLayout
	{
		StringLayout consoleLayout(Configuration configuration);

		StringLayout fileLayout(Configuration configuration);
	}

	public static final class Log4jTextLayout implements Log4jLayout
	{
		private final boolean color;

		public Log4jTextLayout(boolean color)
		{
			this.color = color;
		}

		@Override
		public StringLayout consoleLayout(Configuration configuration)
		{
			if (color)
				return PatternLayout.newBuilder().withPattern(
						"%highlight{%p %t - %C{1}.%M(%L) | %m}{FATAL=red, ERROR=red, WARN=yellow, INFO=white, DEBUG=white, TRACE=white}%n")
						.build();
			else
				return PatternLayout.newBuilder().withPattern("%p %t - %C{1}.%M(%L) | %m%n").build();
		}

		@Override
		public StringLayout fileLayout(Configuration configuration)
		{
			return PatternLayout.newBuilder().withPattern("%d [%t] %-5p %c - %m%n").build();
		}
	}

	public static final class Log4jTextMdcLayout implements Log4jLayout
	{
		private final boolean color;

		public Log4jTextMdcLayout(boolean color)
		{
			this.color = color;
		}

		@Override
		public StringLayout consoleLayout(Configuration configuration)
		{
			if (color)
				return PatternLayout.newBuilder().withPattern(
						"%highlight{%p %t - %C{1}.%M(%L)%notEmpty{ - %X} | %m}{FATAL=red, ERROR=red, WARN=yellow, INFO=white, DEBUG=white, TRACE=white}%n")
						.build();
			else
				return PatternLayout.newBuilder().withPattern("%p %t - %C{1}.%M(%L)%notEmpty{ - %X} | %m%n").build();
		}

		@Override
		public StringLayout fileLayout(Configuration configuration)
		{
			return PatternLayout.newBuilder().withPattern("%d [%t] %-5p %c%notEmpty{ - %X} - %m%n").build();
		}
	}

	public static final class Log4jJsonLayout implements Log4jLayout
	{
		public static enum TemplateUri
		{
			ECS("classpath:EcsLayout.json"), GCP("classpath:GcpLayout.json"), GELF(
					"classpath:GelfLayout.json"), LOGSTASH("classpath:LogstashJsonEventLayoutV1.json");

			private final String uri;

			private TemplateUri(String uri)
			{
				this.uri = uri;
			}

			public String getUri()
			{
				return uri;
			}
		}

		private final TemplateUri templateUri;

		public Log4jJsonLayout(TemplateUri templateUri)
		{
			this.templateUri = templateUri;
		}

		@Override
		public StringLayout consoleLayout(Configuration configuration)
		{
			return JsonTemplateLayout.newBuilder().setConfiguration(configuration)
					.setEventTemplateUri(templateUri.getUri()).build();
		}

		@Override
		public StringLayout fileLayout(Configuration configuration)
		{
			return JsonTemplateLayout.newBuilder().setConfiguration(configuration)
					.setEventTemplateUri(templateUri.getUri()).build();
		}
	}

	public Log4jConfiguration(LoggerContext loggerContext, String name, String fileNamePart, boolean consoleOutEnabled,
			Log4jLayout consoleOutLayout, Level consoleOutLevel, boolean consoleErrEnabled,
			Log4jLayout consoleErrLayout, Level consoleErrLevel, boolean fileEnabled, Log4jLayout fileLayout,
			Level fileLevel)
	{
		super(loggerContext, ConfigurationSource.NULL_SOURCE);

		if (name != null)
			setName(name);

		addLogger("dev.dsf", min(consoleOutLevel, consoleErrLevel, fileLevel));
		addLogger("org.eclipse.jetty", Level.INFO);
		addLogger("ca.uhn.fhir.parser.LenientErrorHandler", Level.ERROR);

		LoggerConfig root = getRootLogger();
		root.setLevel(Level.WARN);

		if (consoleOutEnabled)
		{
			Appender console = ConsoleAppender.newBuilder().setName("CONSOLE.OUT").setTarget(Target.SYSTEM_OUT)
					.setLayout(consoleOutLayout.consoleLayout(this)).build();
			addAppender(console);
			root.addAppender(console, consoleOutLevel, null);
		}

		if (consoleErrEnabled)
		{
			Appender console = ConsoleAppender.newBuilder().setName("CONSOLE.ERR").setTarget(Target.SYSTEM_ERR)
					.setLayout(consoleErrLayout.consoleLayout(this)).build();
			addAppender(console);
			root.addAppender(console, consoleErrLevel, null);
		}

		if (fileEnabled)
		{
			Appender file = RollingFileAppender.newBuilder().setName("FILE")
					.withFileName("log/" + fileNamePart + ".log")
					.withFilePattern("log/" + fileNamePart + "_%d{yyyy-MM-dd}_%i.log.gz").setIgnoreExceptions(false)
					.setLayout(fileLayout.fileLayout(this))
					.withPolicy(CompositeTriggeringPolicy.createPolicy(OnStartupTriggeringPolicy.createPolicy(1),
							TimeBasedTriggeringPolicy.newBuilder().build()))
					.build();
			addAppender(file);
			root.addAppender(file, fileLevel, null);
		}
	}

	private Level min(Level... levels)
	{
		return Stream.of(levels).sorted(Comparator.comparing(Level::intLevel).reversed()).findFirst().get();
	}

	protected void addLogger(String loggerName, Level level)
	{
		LoggerConfig config = new LoggerConfig();
		config.setLevel(level);

		addLogger(loggerName, config);
	}

	protected void addSpecialLogger(String name, String fileNamePart, Function<Configuration, StringLayout> fileLayout,
			Function<Configuration, StringLayout> outLayout, Function<Configuration, StringLayout> errLayout,
			Level level)
	{
		String loggerName = "dsf-" + name + "-logger";
		String appenderName = name.toUpperCase(Locale.ENGLISH);
		fileNamePart = fileNamePart + "-" + name;

		Appender file = createFileAppender(appenderName, fileNamePart, fileLayout.apply(this));
		Appender out = createConsoleAppender(appenderName, Target.SYSTEM_OUT, outLayout.apply(this));
		Appender err = createConsoleAppender(appenderName, Target.SYSTEM_ERR, errLayout.apply(this));

		LoggerConfig config = new LoggerConfig();
		config.setLevel(file == null && out == null && err == null ? Level.OFF : level);
		config.setAdditive(false);

		if (file != null)
		{
			addAppender(file);
			config.addAppender(file, null, null);
		}
		if (out != null)
		{
			addAppender(out);
			config.addAppender(out, null, null);
		}
		if (err != null)
		{
			addAppender(err);
			config.addAppender(err, null, null);
		}

		addLogger(loggerName, config);
	}

	private Appender createFileAppender(String appenderName, String fileNamePart, StringLayout layout)
	{
		if (layout == null)
			return null;

		return RollingFileAppender.newBuilder().setName(appenderName + ".FILE")
				.withFileName("log/" + fileNamePart + ".log")
				.withFilePattern("log/" + fileNamePart + "_%d{yyyy-MM-dd}_%i.log.gz").setIgnoreExceptions(false)
				.setLayout(layout)
				.withPolicy(CompositeTriggeringPolicy.createPolicy(OnStartupTriggeringPolicy.createPolicy(1),
						TimeBasedTriggeringPolicy.newBuilder().build()))
				.build();
	}

	private Appender createConsoleAppender(String appenderName, Target target, StringLayout layout)
	{
		if (layout == null)
			return null;

		String name = appenderName + "." + switch (target)
		{
			case SYSTEM_OUT -> "OUT";
			case SYSTEM_ERR -> "ERR";
		};

		return ConsoleAppender.newBuilder().setName(name).setTarget(target).setLayout(layout).build();
	}

	@Override
	protected void doConfigure()
	{
		// nothing to configure
	}
}
