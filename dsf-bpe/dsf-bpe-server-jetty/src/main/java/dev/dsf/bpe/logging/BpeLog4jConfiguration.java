package dev.dsf.bpe.logging;

import java.util.EnumSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.OnStartupTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.layout.PatternLayout;

import dev.dsf.common.logging.Log4jConfiguration;

public class BpeLog4jConfiguration extends Log4jConfiguration
{
	private static final PatternLayout DATA_LAYOUT = PatternLayout.newBuilder().withPattern("%d%notEmpty{ %X} %m%n")
			.build();

	public static enum Data
	{
		OUT(Target.SYSTEM_OUT), ERROR(Target.SYSTEM_ERR), FILE(null), OFF(null);

		private final Target target;

		private Data(Target target)
		{
			this.target = target;
		}

		public Target getTarget()
		{
			return target;
		}
	}

	public BpeLog4jConfiguration(LoggerContext loggerContext, String name, String fileNamePart,
			Log4jLayout consoleLayout, Level consoleLevel, Log4jLayout fileLayout, Level fileLevel, Data data)
	{
		super(loggerContext, name, fileNamePart, consoleLayout, consoleLevel, fileLayout, fileLevel);

		if (EnumSet.of(Data.OUT, Data.ERROR, Data.FILE).contains(data))
		{
			Appender dataAppender = Data.FILE.equals(data) ? createFileAppender(fileNamePart)
					: createConsoleAppender(data);

			addAppender(dataAppender);
			addLogger("dsf-data-logger", Level.DEBUG, dataAppender, false);
		}
		else
			addLogger("dsf-data-logger", Level.OFF);
	}

	private Appender createFileAppender(String fileNamePart)
	{
		return RollingFileAppender.newBuilder().setName("DATA").withFileName("log/" + fileNamePart + "-data.log")
				.withFilePattern("log/" + fileNamePart + "-data_%d{yyyy-MM-dd}_%i.log.gz").setIgnoreExceptions(false)
				.setLayout(DATA_LAYOUT)
				.withPolicy(CompositeTriggeringPolicy.createPolicy(OnStartupTriggeringPolicy.createPolicy(1),
						TimeBasedTriggeringPolicy.newBuilder().build()))
				.build();
	}

	private Appender createConsoleAppender(Data data)
	{
		return ConsoleAppender.newBuilder().setName("DATA").setTarget(data.getTarget()).setLayout(DATA_LAYOUT).build();
	}
}
