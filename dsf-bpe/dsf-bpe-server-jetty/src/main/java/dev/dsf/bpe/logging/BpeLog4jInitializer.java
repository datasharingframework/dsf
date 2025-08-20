package dev.dsf.bpe.logging;

import java.util.function.Function;

import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Configuration;

import dev.dsf.common.logging.Log4jConfigurationFactory;
import dev.dsf.common.logging.Log4jInitializer;

public class BpeLog4jInitializer extends Log4jInitializer
{
	public static final String LOG_DATA_FILE = "dev.dsf.log.data.file";
	public static final String LOG_DATA_CONSOLE_OUT = "dev.dsf.log.data.console.out";
	public static final String LOG_DATA_CONSOLE_ERR = "dev.dsf.log.data.console.err";

	private final Function<Configuration, StringLayout> specialFile;
	private final Function<Configuration, StringLayout> specialConsoleOut;
	private final Function<Configuration, StringLayout> specialConsoleErr;

	public BpeLog4jInitializer()
	{
		specialFile = getSpecial(LOG_DATA_FILE, SPECIAL_OFF);
		specialConsoleOut = getSpecial(LOG_DATA_CONSOLE_OUT, SPECIAL_OFF);
		specialConsoleErr = getSpecial(LOG_DATA_CONSOLE_ERR, SPECIAL_OFF);
	}

	@Override
	protected Log4jConfigurationFactory createLog4jConfigurationFactory()
	{
		return new Log4jConfigurationFactory((loggerContext, name) -> new BpeLog4jConfiguration(loggerContext, name,
				"bpe", consoleOutLayout, consoleOutLevel, consoleErrLayout, consoleErrLevel, fileLayout, fileLevel,
				specialFile, specialConsoleOut, specialConsoleErr));
	}
}
