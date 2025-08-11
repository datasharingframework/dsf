package dev.dsf.fhir.logging;

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

public class FhirLog4jConfiguration extends Log4jConfiguration
{
	private static final PatternLayout AUDIT_LAYOUT = PatternLayout.newBuilder().withPattern("%d [%t] %-5p %c - %m%n")
			.build();

	public static enum Audit
	{
		OUT(Target.SYSTEM_OUT), ERROR(Target.SYSTEM_ERR), FILE(null), OFF(null);

		private final Target target;

		private Audit(Target target)
		{
			this.target = target;
		}

		public Target getTarget()
		{
			return target;
		}
	}

	public FhirLog4jConfiguration(LoggerContext loggerContext, String name, String fileNamePart,
			Log4jLayout consoleLayout, Level consoleLevel, Log4jLayout fileLayout, Level fileLevel, Audit audit)
	{
		super(loggerContext, name, fileNamePart, consoleLayout, consoleLevel, fileLayout, fileLevel);

		if (EnumSet.of(Audit.OUT, Audit.ERROR, Audit.FILE).contains(audit))
		{
			Appender auditAppender = Audit.FILE.equals(audit) ? createFileAppender(fileNamePart)
					: createConsoleAppender(audit);

			addAppender(auditAppender);
			addLogger("dsf-audit-logger", Level.INFO, auditAppender);
		}
		else
			addLogger("dsf-audit-logger", Level.OFF);
	}

	private Appender createFileAppender(String fileNamePart)
	{
		return RollingFileAppender.newBuilder().setName("AUDIT").withFileName("log/" + fileNamePart + "-audit.log")
				.withFilePattern("log/" + fileNamePart + "-audit_%d{yyyy-MM-dd}_%i.log.gz").setIgnoreExceptions(false)
				.setLayout(AUDIT_LAYOUT)
				.withPolicy(CompositeTriggeringPolicy.createPolicy(OnStartupTriggeringPolicy.createPolicy(1),
						TimeBasedTriggeringPolicy.newBuilder().build()))
				.build();
	}

	private Appender createConsoleAppender(Audit audit)
	{
		return ConsoleAppender.newBuilder().setName("CONSOLE").setTarget(audit.getTarget()).setLayout(AUDIT_LAYOUT)
				.build();
	}
}
