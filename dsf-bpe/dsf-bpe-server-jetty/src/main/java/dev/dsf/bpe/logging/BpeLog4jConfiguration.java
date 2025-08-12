package dev.dsf.bpe.logging;

import java.util.function.Function;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;

import dev.dsf.common.logging.Log4jConfiguration;

public class BpeLog4jConfiguration extends Log4jConfiguration
{
	public BpeLog4jConfiguration(LoggerContext loggerContext, String name, String fileNamePart,
			Log4jLayout consoleOutLayout, Level consoleOutLevel, Log4jLayout consoleErrLayout, Level consoleErrLevel,
			Log4jLayout fileLayout, Level fileLevel, Function<Configuration, StringLayout> dataFile,
			Function<Configuration, StringLayout> dataOut, Function<Configuration, StringLayout> dataErr)
	{
		super(loggerContext, name, fileNamePart, consoleOutLayout, consoleOutLevel, consoleErrLayout, consoleErrLevel,
				fileLayout, fileLevel);

		addSpecialLogger("data", fileNamePart, dataFile, dataOut, dataErr, Level.DEBUG);
	}
}
