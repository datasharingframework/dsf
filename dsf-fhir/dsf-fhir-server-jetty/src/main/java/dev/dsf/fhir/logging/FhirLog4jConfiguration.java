package dev.dsf.fhir.logging;

import java.util.function.Function;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;

import dev.dsf.common.logging.Log4jConfiguration;

public class FhirLog4jConfiguration extends Log4jConfiguration
{
	public FhirLog4jConfiguration(LoggerContext loggerContext, String name, String fileNamePart,
			Log4jLayout consoleOutLayout, Level consoleOutLevel, Log4jLayout consoleErrLayout, Level consoleErrLevel,
			Log4jLayout fileLayout, Level fileLevel, Function<Configuration, StringLayout> auditFile,
			Function<Configuration, StringLayout> auditOut, Function<Configuration, StringLayout> auditErr)
	{
		super(loggerContext, name, fileNamePart, consoleOutLayout, consoleOutLevel, consoleErrLayout, consoleErrLevel,
				fileLayout, fileLevel);

		addSpecialLogger("audit", fileNamePart, auditFile, auditOut, auditErr, Level.INFO);
	}
}
