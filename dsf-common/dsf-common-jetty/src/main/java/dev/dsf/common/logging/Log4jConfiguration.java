package dev.dsf.common.logging;

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
		@Override
		public StringLayout consoleLayout(Configuration configuration)
		{
			return PatternLayout.newBuilder().withPattern(
					"%highlight{%p %t - %C{1}.%M(%L) | %m}{FATAL=red, ERROR=red, WARN=yellow, INFO=white, DEBUG=white, TRACE=white}%n")
					.build();
		}

		@Override
		public StringLayout fileLayout(Configuration configuration)
		{
			return PatternLayout.newBuilder().withPattern("%d [%t] %-5p %c - %m%n").build();
		}
	}

	public static final class Log4jTextMdcLayout implements Log4jLayout
	{
		@Override
		public StringLayout consoleLayout(Configuration configuration)
		{
			return PatternLayout.newBuilder().withPattern(
					"%highlight{%p %t - %C{1}.%M(%L)%notEmpty{ - %X} | %m}{FATAL=red, ERROR=red, WARN=yellow, INFO=white, DEBUG=white, TRACE=white}%n")
					.build();
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

	public Log4jConfiguration(LoggerContext loggerContext, String name, String fileNamePart, Log4jLayout consoleLayout,
			Level consoleLevel, Log4jLayout fileLayout, Level fileLevel)
	{
		super(loggerContext, ConfigurationSource.NULL_SOURCE);

		if (name != null)
			setName(name);

		addLogger("dev.dsf", min(consoleLevel, fileLevel));
		addLogger("org.eclipse.jetty", Level.INFO);
		addLogger("ca.uhn.fhir.parser.LenientErrorHandler", Level.ERROR);

		LoggerConfig root = getRootLogger();
		root.setLevel(Level.WARN);

		if (!Level.OFF.equals(consoleLevel))
		{
			Appender console = ConsoleAppender.newBuilder().setName("CONSOLE").setTarget(Target.SYSTEM_OUT)
					.setLayout(consoleLayout.consoleLayout(this)).build();
			addAppender(console);
			root.addAppender(console, consoleLevel, null);
		}

		if (!Level.OFF.equals(fileLevel))
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

	private Level min(Level a, Level b)
	{
		return a.isLessSpecificThan(b) ? a : b;
	}

	protected void addLogger(String loggerName, Level level)
	{
		addLogger(loggerName, level, null);
	}

	protected void addLogger(String loggerName, Level level, Appender appender)
	{
		LoggerConfig c = new LoggerConfig();
		c.setLevel(level);

		if (appender != null)
			c.addAppender(appender, null, null);

		addLogger(loggerName, c);
	}

	@Override
	protected void doConfigure()
	{
		// nothing to configure
	}
}
